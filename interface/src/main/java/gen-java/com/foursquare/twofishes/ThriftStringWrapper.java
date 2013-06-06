/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package com.foursquare.twofishes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.thrift.*;
import org.apache.thrift.async.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;

// No additional import required for struct/union.

public class ThriftStringWrapper implements TBase<ThriftStringWrapper, ThriftStringWrapper._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("ThriftStringWrapper");

  private static final TField STR_FIELD_DESC = new TField("str", TType.STRING, (short)1);

  public String str;

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements TFieldIdEnum {
    STR((short)1, "str");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // STR
          return STR;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments

  public static final Map<_Fields, FieldMetaData> metaDataMap;
  static {
    Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.STR, new FieldMetaData("str", TFieldRequirementType.OPTIONAL, 
        new FieldValueMetaData(TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(ThriftStringWrapper.class, metaDataMap);
  }

  public ThriftStringWrapper() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ThriftStringWrapper(ThriftStringWrapper other) {
    if (other.isSetStr()) {
      this.str = other.str;
    }
  }

  public ThriftStringWrapper deepCopy() {
    return new ThriftStringWrapper(this);
  }

  @Override
  public void clear() {
    this.str = null;
  }

  public String getStr() {
    return this.str;
  }

  public ThriftStringWrapper setStr(String str) {
    this.str = str;
    return this;
  }

  public void unsetStr() {
    this.str = null;
  }

  /** Returns true if field str is set (has been asigned a value) and false otherwise */
  public boolean isSetStr() {
    return this.str != null;
  }

  public void setStrIsSet(boolean value) {
    if (!value) {
      this.str = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case STR:
      if (value == null) {
        unsetStr();
      } else {
        setStr((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case STR:
      return getStr();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case STR:
      return isSetStr();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ThriftStringWrapper)
      return this.equals((ThriftStringWrapper)that);
    return false;
  }

  public boolean equals(ThriftStringWrapper that) {
    if (that == null)
      return false;

    boolean this_present_str = true && this.isSetStr();
    boolean that_present_str = true && that.isSetStr();
    if (this_present_str || that_present_str) {
      if (!(this_present_str && that_present_str))
        return false;
      if (!this.str.equals(that.str))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(ThriftStringWrapper other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    ThriftStringWrapper typedOther = (ThriftStringWrapper)other;

    lastComparison = Boolean.valueOf(isSetStr()).compareTo(typedOther.isSetStr());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStr()) {
      lastComparison = TBaseHelper.compareTo(this.str, typedOther.str);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(TProtocol iprot) throws TException {
    TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 1: // STR
          if (field.type == TType.STRING) {
            this.str = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();

    // check for required fields of primitive type, which can't be checked in the validate method
    validate();
  }

  public void write(TProtocol oprot) throws TException {
    validate();

    oprot.writeStructBegin(STRUCT_DESC);
    if (this.str != null) {
      if (isSetStr()) {
        oprot.writeFieldBegin(STR_FIELD_DESC);
        oprot.writeString(this.str);
        oprot.writeFieldEnd();
      }
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ThriftStringWrapper(");
    boolean first = true;

    if (isSetStr()) {
      sb.append("str:");
      if (this.str == null) {
        sb.append("null");
      } else {
        sb.append(this.str);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws TException {
    // check for required fields
  }

}
