/* 
 * Copyright (c) 2012 Orderly Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.serde;

// Java
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Hive
import org.apache.hadoop.hive.serde2.SerDeException;

/**
 * CfLogStruct represents the Hive struct for a row in a CloudFront access log.
 *
 * Contains a parse() method to perform an update-in-place for this instance
 * based on the current row's contents.
 *
 * Constructor is empty because we do updates-in-place for performance reasons.
 * An immutable Scala case class would be nice but fear it would be s-l-o-w
 */
class CfLogStruct() {

  // -------------------------------------------------------------------------------------------------------------------
  // Mutable properties for this Hive struct
  // -------------------------------------------------------------------------------------------------------------------

  public String dt;
  public String edgelocation;
  public Integer bytessent; 
  public String ipaddress;
  public String operation;
  public String domain;
  public String objct;
  public Integer httpstatus;
  public String referrer;
  public String useragent;
  public String querystring;
  // var querymap: Map[String, String] TODO add this

  // -------------------------------------------------------------------------------------------------------------------
  // Static configuration
  // -------------------------------------------------------------------------------------------------------------------

  // Define the regular expression for extracting the fields
  // Adapted from Amazon's own cloudfront-loganalyzer.tgz
  private static final String w = "[\\s]+"; // Whitespace regex
  private static final Pattern cfRegex = Pattern.compile("([\\S]+)"  // Date          / date
                                                   + w + "([\\S]+)"  // Time          / time
                                                   + w + "([\\S]+)"  // EdgeLocation  / x-edge-location
                                                   + w + "([\\S]+)"  // BytesSent     / sc-bytes
                                                   + w + "([\\S]+)"  // IPAddress     / c-ip
                                                   + w + "([\\S]+)"  // Operation     / cs-method
                                                   + w + "([\\S]+)"  // Domain        / cs(Host)
                                                   + w + "([\\S]+)"  // Object        / cs-uri-stem
                                                   + w + "([\\S]+)"  // HttpStatus    / sc-status
                                                   + w + "([\\S]+)"  // Referrer      / cs(Referer)
                                                   + w + "([\\S]+)"  // UserAgent     / cs(User Agent)
                                                   + w + "(.+)");    // Querystring   / cs-uri-query

  // To handle the CloudFront DateTime format
  private static final SimpleDateFormat cfDateFormat = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss ZZZZZ");
  private static final SimpleDateFormat hiveDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

  // -------------------------------------------------------------------------------------------------------------------
  // Deserialization logic
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Parses the input row String into a Java object.
   * For performance reasons this works in-place updating the fields
   * within this CfLogStruct, rather than creating a new one.
   * 
   * @param row The raw String containing the row contents
   * @return This struct with all values updated
   * @throws SerDeException For any exception during parsing
   */
  // TODO: need to update this to Java.
  public Object parse(row: String) throws SerDeException {
    
    // Check our row is kosher
    row match {
      case CfRegex(date, time, edgelocation, bytessent, ipaddress, operation, domain, objct, httpstatus, referrer, useragent, querystring) =>
        this.dt = toHiveDate(date + " " + time)
        this.edgelocation = edgelocation
        this.bytessent = toInt(bytessent)
        this.ipaddress = ipaddress
        this.operation = operation
        this.domain = domain
        this.objct = objct
        this.httpstatus = toInt(httpstatus)
        this.referrer = referrer
        this.useragent = useragent
        this.querystring = querystring
        // TODO: build the querymap too
      case _ => throw new SerDeException("CloudFront regexp did not match: %s".format(row))
    }

    return this; // Return the CfLogStruct
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Datatype conversions
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Implicit conversion from String to Integer. To deal with
   * the fact that AWS uses a single "-"" for null.
   *
   * @param s The String to check
   * @return The Integer, or null if the String was "-" 
   */
  private Integer toInt(s: String) {
    return (s.compareTo("-") == 0) ? null : Integer.valueOf(s);
  }

  /**
   * Explicit conversion to turn a "-" String into null.
   * Useful for "-" URIs (URI is set to "-" if e.g. S3 is accessed
   * from a file:// protocol).
   *
   * @param s The String to check
   * @return The original String, or null if the String was "-" 
   */
  private String nullifyHyphen(s: String) {
    return (s.compareTo("-") == 0) ? null : s;
  }

  /**
   * Convert a date from CloudFront format to Hive format
   *
   * @param dt The datetime in CloudFront String format
   * @return The datetime in Hive-friendly String format
   */
  private String toHiveDate(dt: String) {
    return hiveDateFormat.format(cfDateFormat.parse(dt).getTime());
  }
}