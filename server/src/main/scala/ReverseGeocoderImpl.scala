//  Copyright 2012 Foursquare Labs Inc. All Rights Reserved
package com.foursquare.twofishes

import com.foursquare.geo.shapes.Gridifier
import com.foursquare.twofishes.Identity._
import com.foursquare.twofishes.util.{GeoTools, GeometryUtils, StoredFeatureId, TwofishesLogger}
import com.foursquare.twofishes.util.Lists.Implicits._
import com.twitter.ostrich.stats.Stats
import com.twitter.util.Duration
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory, Point => JTSPoint}
import com.vividsolutions.jts.io.{WKBReader, WKTWriter}
import com.vividsolutions.jts.util.GeometricShapeFactory
import org.bson.types.ObjectId
import scala.collection.mutable.ListBuffer
import scalaj.collection.Implicits._

class ReverseGeocodeParseOrdering extends Ordering[Parse[Sorted]] {
  def compare(a: Parse[Sorted], b: Parse[Sorted]): Int = {
    val comparisonOpt = for {
      aFeatureMatch <- a.headOption
      bFeatureMatch <- b.headOption
    } yield {
      val aServingFeature = aFeatureMatch.fmatch
      val bServingFeature = bFeatureMatch.fmatch
      val aWoeTypeOrder = YahooWoeTypes.getOrdering(aServingFeature.feature.woeType)
      val bWoeTypeOrder = YahooWoeTypes.getOrdering(bServingFeature.feature.woeType)
      if (aWoeTypeOrder != bWoeTypeOrder) {
         aWoeTypeOrder - bWoeTypeOrder
      } else {
        bServingFeature.scoringFeatures.boost -
          aServingFeature.scoringFeatures.boost
      }
    }

    comparisonOpt.getOrElse(0)
  }
}

trait TimeResponseHelper {
  def timeResponse(ostrichKey: String)(f: GeocodeResponse) = {
    val (rv, duration) = Duration.inNanoseconds(f)
    Stats.addMetric(ostrichKey + "_usec", duration.inMicroseconds.toInt)
    Stats.addMetric(ostrichKey + "_msec", duration.inMilliseconds.toInt)
    if (rv.interpretations.size > 0) {
      Stats.addMetric(ostrichKey + "_with_results_usec", duration.inMicroseconds.toInt)
      Stats.addMetric(ostrichKey + "_with_results_msec", duration.inMilliseconds.toInt)
    }
    rv
  }
}

class ReverseGeocoderHelperImpl(
  store: GeocodeStorageReadService,
  req: CommonGeocodeRequestParams,
  logger: MemoryLogger
) extends GeocoderImplTypes with TimeResponseHelper {
  def featureGeometryIntersections(wkbGeometry: Array[Byte], otherGeom: Geometry) = {
    val wkbReader = new WKBReader()
    val geom = wkbReader.read(wkbGeometry)
    (geom, geom.intersects(otherGeom))
  }

  def computeCoverage(
    featureGeometry: Geometry,
    requestGeometry: Geometry
  ): Double = {
    val intersection = featureGeometry.intersection(requestGeometry)
    math.min(1, intersection.getArea() / requestGeometry.getArea())
  }

  def responseIncludes(include: ResponseIncludes): Boolean =
    GeocodeRequestUtils.responseIncludes(req, include)

  def findMatches(
    otherGeom: Geometry,
    cellGeometries: Seq[CellGeometry]
  ): Seq[StoredFeatureId] = {
    if (req.debug > 0) {
      logger.ifDebug("had %d candidates", cellGeometries.size)
      // logger.ifDebug("s2 cells: %s", cellids)
    }

    val matches = new ListBuffer[StoredFeatureId]()
    print(req.woeRestrict)

    for {
      cellGeometry <- cellGeometries
      if (req.woeRestrict.isEmpty || req.woeRestrict.asScala.has(cellGeometry.woeType))
      val oid = new ObjectId(cellGeometry.getOid())
      fid <- StoredFeatureId.fromLegacyObjectId(oid)
    } yield {
      if (!matches.has(fid)) {
        if (cellGeometry.isFull) {
          logger.ifDebug("was full: %s", fid)
          matches.append(fid)
        } else if (cellGeometry.wkbGeometry != null) {
          val (geom, intersects) = logger.logDuration("intersectionCheck", "intersecting %s".format(fid)) {
            featureGeometryIntersections(cellGeometry.getWkbGeometry(), otherGeom)
          }
          if (intersects) {
            matches.append(fid)
          } else {
          }
        } else {
          logger.ifDebug("not full and no geometry for: %s", fid)
        }
      }
    }

    matches.toSeq
  }

  def doBulkReverseGeocode(otherGeoms: Seq[Geometry]): Map[Int, Seq[GeocodeInterpretation]] = {
    val geomIndexToCellIdMap: Map[Int, Seq[Long]] = (for {
      (g, index) <- otherGeoms.zipWithIndex
    } yield { index -> Gridifier.coverAtAllLevels(store.getSizes, g, false) }).toMap

    val cellGeometryMap: Map[Long, Seq[CellGeometry]] =
      (for {
        cellid: Long <- geomIndexToCellIdMap.values.flatten.toSet
      } yield {
        cellid -> store.getByS2CellId(cellid)
      }).toMap
    
    (for {
      (otherGeom, index) <- otherGeoms.zipWithIndex
    } yield {
      val cellGeometries = geomIndexToCellIdMap(index).flatMap(cellid => cellGeometryMap(cellid))

      val featureOids = findMatches(otherGeom, cellGeometries)

      val servingFeaturesMap: Map[StoredFeatureId, GeocodeServingFeature] =
        store.getByFeatureIds(featureOids.toSet.toList)

      // need to get polygons if we need to calculate coverage
      val polygonMap: Map[StoredFeatureId, Array[Byte]] =
        if (GeocodeRequestUtils.shouldFetchPolygon(req)) {
          store.getPolygonByFeatureIds(featureOids)
        } else { Map.empty }

      val wkbReader = new WKBReader()
      // for each, check if we're really in it
      val parses: SortedParseSeq = servingFeaturesMap.map({ case (oid, f) => {
        val parse = Parse[Sorted](List(FeatureMatch(0, 0, "", f)))
        if (responseIncludes(ResponseIncludes.REVGEO_COVERAGE) &&
            otherGeom.getNumPoints > 2) {
          polygonMap.get(oid).foreach(wkb => {
            val geom = wkbReader.read(wkb)
            if (geom.getNumPoints > 2) {
              parse.scoringFeatures.setPercentOfRequestCovered(computeCoverage(geom, otherGeom))
              parse.scoringFeatures.setPercentOfFeatureCovered(computeCoverage(otherGeom, geom))
            }
          })
        }
        parse
      }}).toSeq

      val parseParams = ParseParams()

      val maxInterpretations = if (req.maxInterpretations <= 0) {
        parses.size
      } else {
        req.maxInterpretations
      }

      val sortedParses = parses.sorted(new ReverseGeocodeParseOrdering).take(maxInterpretations)
      val responseProcessor = new ResponseProcessor(req, store, logger)
      val interpretations = responseProcessor.hydrateParses(sortedParses, parseParams, polygonMap,
        fixAmbiguousNames = false)

      (index -> interpretations)
    }).toMap
  }
}

class ReverseGeocoderImpl(
  store: GeocodeStorageReadService,
  req: GeocodeRequest
) extends GeocoderImplTypes with TimeResponseHelper {
  val logger = new MemoryLogger(req)
  val commonParams = GeocodeRequestUtils.geocodeRequestToCommonRequestParams(req)
  val reverseGeocoder =
    new ReverseGeocoderHelperImpl(store, commonParams, logger)

  def doSingleReverseGeocode(geom: Geometry): GeocodeResponse = {
    val interpretations = reverseGeocoder.doBulkReverseGeocode(List(geom))(0)
    val response = ResponseProcessor.generateResponse(req.debug, logger, interpretations)
    if (req.debug > 0) {
      val wktWriter = new WKTWriter
      response.setRequestWktGeometry(wktWriter.write(geom))
    }
    response
  }

  def reverseGeocodePoint(ll: GeocodePoint): GeocodeResponse = {
    val geomFactory = new GeometryFactory()
    val point = geomFactory.createPoint(new Coordinate(ll.lng, ll.lat))
    doSingleReverseGeocode(point)
  }


  def doGeometryReverseGeocode(geom: Geometry) = {
    doSingleReverseGeocode(geom)
  }

  def reverseGeocode(): GeocodeResponse = {
    Stats.incr("revgeo-requests", 1)
    if (req.ll != null) {
      if (req.isSetRadius && req.radius > 0) {
        if (req.radius > 50000) {
          println("too large revgeo: " + req)
          //throw new Exception("radius too big (%d > %d)".format(req.radius, maxRadius))
          new GeocodeResponse()
        } else {
          val sizeDegrees = req.radius / 111319.9
          val gsf = new GeometricShapeFactory()
          gsf.setSize(sizeDegrees)
          gsf.setNumPoints(100)
          gsf.setCentre(new Coordinate(req.ll.lng, req.ll.lat))
          val geom = gsf.createCircle()
          timeResponse("revgeo-geom") {
            doGeometryReverseGeocode(geom)
          }
        }
      } else {
        timeResponse("revgeo-point") {
          reverseGeocodePoint(req.ll)
        }
      }
    } else if (req.bounds != null) {
      val s2rect = GeoTools.boundingBoxToS2Rect(req.bounds)
      val geomFactory = new GeometryFactory()
      val geom = geomFactory.createLinearRing(Array(
        new Coordinate(s2rect.lng.lo, s2rect.lat.lo),
        new Coordinate(s2rect.lng.hi, s2rect.lat.lo),
        new Coordinate(s2rect.lng.hi, s2rect.lat.hi),
        new Coordinate(s2rect.lng.hi, s2rect.lat.lo),
        new Coordinate(s2rect.lng.lo, s2rect.lat.lo)
      ))
      Stats.time("revgeo-geom") {
        doGeometryReverseGeocode(geom)
      }
    } else {
      throw new Exception("no bounds or ll")
    }
  }
}

class BulkReverseGeocoderImpl(
  store: GeocodeStorageReadService,
  req: BulkReverseGeocodeRequest
) extends GeocoderImplTypes with TimeResponseHelper {
  val logger = new MemoryLogger(req.params)
  val reverseGeocoder =
    new ReverseGeocoderHelperImpl(store, Option(req.params).getOrElse(new CommonGeocodeRequestParams()), logger)

  def reverseGeocode(): BulkReverseGeocodeResponse = {
    Stats.incr("bulk-revgeo-requests", 1)

    val geomFactory = new GeometryFactory()

    val points = req.latlngs.asScala.map(ll => geomFactory.createPoint(new Coordinate(ll.lng, ll.lat)))
    val response = new BulkReverseGeocodeResponse()
      .setInterpretationMap(
        reverseGeocoder.doBulkReverseGeocode(points).mapValues(_.toList.asJava).asJava
      )

    if (req.params.debug > 0) {
      response.setDebugLines(List[String]().asJava)
    }
    response
  }
}