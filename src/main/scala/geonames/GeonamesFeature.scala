// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.
package com.foursquare.geocoder.geonames

import com.foursquare.geocoder.{Helpers, LogHelper}

object GeonamesFeatureColumns extends Enumeration {
   type GeonamesFeatureColumns = Value
   val GEONAMEID, PLACE_NAME, NAME, ASCIINAME, ALTERNATENAMES, LATITUDE, LONGITUDE,
      FEATURE_CLASS, FEATURE_CODE, COUNTRY_CODE, CC2, ADMIN1_CODE, ADMIN2_CODE, ADMIN3_CODE,
      ADMIN4_CODE, ADMIN1_NAME, ADMIN2_NAME, ADMIN3_NAME, POPULATION, ELEVATION, GTOPO30, TIMEZONE,
      MODIFICATION_DATE, ACCURACY = Value
}

import GeonamesFeatureColumns._

object GeonamesFeature extends LogHelper with Helpers {
  val adminColumns = List(
    GEONAMEID,
    NAME,
    ASCIINAME,
    ALTERNATENAMES,
    LATITUDE,
    LONGITUDE,
    FEATURE_CLASS,
    FEATURE_CODE,
    COUNTRY_CODE,
    CC2,
    ADMIN1_CODE,
    ADMIN2_CODE,
    ADMIN3_CODE,
    ADMIN4_CODE,
    POPULATION,
    ELEVATION,
    GTOPO30,
    TIMEZONE,
    MODIFICATION_DATE
  )

  def parseFromAdminLine(index: Int, line: String): Option[GeonamesFeature] = {
    val parts = line.split("\t")
    if (parts.size != adminColumns.size) {
      logger.error("line %d has the wrong number of columns. Has %d, needs %d".format(
        index, parts.size, adminColumns.size))
      None
    } else {
      val colMap = adminColumns.zip(parts).toMap
      val feature = new GeonamesFeature(colMap)
      if (feature.isValid) {
        Some(feature)
      } else {
        None
      }
    }
  }
}

object AdminLevel extends Enumeration {
  type AdminLevel = Value
  val COUNTRY, ADM1, ADM2, ADM3, ADM4, OTHER = Value
}

import AdminLevel._

// http://www.geonames.org/export/codes.html
class GeonamesFeatureClass(featureClass: Option[String], featureCode: Option[String]) {
  def isBuilding = featureClass.exists(_ == "S")
  def isCountry = featureCode.exists(_.contains("PCL"))
  def isAdmin = adminLevel != OTHER

  def adminLevel: AdminLevel.Value = {
    if (isCountry) {
      COUNTRY
    } else {
      featureCode.map(_ match {
        case "ADM1" => ADM1
        case "ADM2" => ADM2
        case "ADM3" => ADM3
        case "ADM4" => ADM4
        case _ => OTHER
      }).getOrElse(OTHER) 
    }
  }
}

class GeonamesFeature(values: Map[GeonamesFeatureColumns.Value, String]) extends Helpers {
  def isValid = {
    values.contains(NAME)
  }
  val featureClass = new GeonamesFeatureClass(values.get(FEATURE_CLASS), values.get(FEATURE_CODE))

  def adminCode(level: AdminLevel.Value): Option[String] = {
    level match {
      case COUNTRY => values.get(COUNTRY_CODE)
      case ADM1 => values.get(ADMIN1_CODE)
      case ADM2 => values.get(ADMIN2_CODE)
      case ADM3 => values.get(ADMIN3_CODE)
      case ADM4 => values.get(ADMIN4_CODE)
      case _ => None
    }
  }

  def makeAdminId(level: AdminLevel.Value): Option[String] = {
    if (adminCode(level).exists(_.nonEmpty)) {
      Some(
        AdminLevel.values.filter(_ <= level).flatMap(l => adminCode(l)).mkString("-")
      )
    } else {
      None
    }
  }

  def parents: List[String] = {
    AdminLevel.values.filter(_ < featureClass.adminLevel).flatMap(l =>
      makeAdminId(l)
    ).toList
  }

  def population: Option[Int] = flattryo {values.get(POPULATION).map(_.toInt)}
  def latitude: Option[Double] = flattryo {values.get(LATITUDE).map(_.toDouble)}
  def longitude: Option[Double] = flattryo {values.get(LONGITUDE).map(_.toDouble)}
  def countryCode: String = values.get(COUNTRY_CODE).getOrElse("XX")
  def name: String = values.getOrElse(NAME, "no name")

  def alternateNames: List[String] =
    values.get(ALTERNATENAMES).toList.flatMap(_.split(",").toList)

  def allNames: List[String] = {
    var names = List(name)
    if (featureClass.isCountry) {
      names ::= countryCode
    }

    names ++= alternateNames
    names
  }
}