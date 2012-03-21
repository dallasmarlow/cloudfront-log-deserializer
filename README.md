# Hive Deserializer for CloudFront access logs

## Introduction

cloudfront-log-deserializer is a Deserializer to import Amazon Web Services' CloudFront access logs into [Apache Hive] [hive] ready for analysis.

This Deserializer serves as the basis for the SnowPlow-specific Deserializers found [in the SnowPlow project] [snowplowserdes] but should be generally useful for anyone wanting to analyse CloudFront access log files in Hive, whether SnowPlow users or not.

cloudfront-log-deserializer is written in Java and is [available] [downloads] from GitHub as a downloadable jarfile. Currently it only supports CloudFront's [download distribution file format] [awslogdocs] (not the streaming file format).

## The CloudFront access log format

Amazon Web Services' CloudFront CDN service supports logging for all access to files within a given distribution. The access log format is different for a CloudFront download distribution versus a streaming distribution, however both use the [W3C extended format] [w3cformat] and contain tab-separated values.

The access log files for a download distribution contain the following fields running left-to-right:

| **Field**         | **Description**                                                                                                                                                                               |
|------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `date `           | The date (UTC) on which the event occurred, e.g. 2009-03-10                                                                                                                                   |
| `time`            | Time when the server finished processing the request (UTC), e.g. 01:42:39                                                                                                                     | 
| `x-edge-location` | The edge location that served the request, e.g. DFW3                                                                                                                                          |
| `sc-bytes`        | Server to client bytes, e.g. 1045619                                                                                                                                                          |
| `c-ip`            | Client IP, e.g. 192.0.2.183                                                                                                                                                                   |
| `cs-method`       | HTTP access method, e.g. GET                                                                                                                                                                  |
| `cs(Host)`        | DNS name (the CloudFront distribution name specified in the request). If you made the request to a CNAME, the DNS name field will contain the underlying distribution DNS name, not the CNAME | 
| `cs-uri-stem`     | URI stem, e.g. /images/daily-ad.jpg                                                                                                                                                           |
| `sc-status`       | HTTP status code, e.g. 200                                                                                                                                                                    |
| `cs(Referer)`     | The referrer, or a single dash (-) if there is no referrer                                                                                                                                    |
| `cs(User Agent)`  | The user agent                                                                                                                                                                                |
| `cs-uri-query`    | The querystring portion of the requested URI, or a single dash (-) if none. Max length is 8kB and encoding standard is RFC 1738 |

For more details on this file format (or indeed the streaming distribution file format), please see the Amazon documentation on [Access Logs] [awslogdocs].

## The Hive table format

cloudfront-log-deserializer maps the access log format for a download distribution very directly onto an equivalent Hive table structure. The only transformation is that the querystring on the accessed URI is converted into a Hive `MAP<STRING, STRING>`.

Here is the Hive table definition in full:

    CREATE EXTERNAL TABLE impressions (
      dt STRING,
      edgelocation STRING,
      bytessent INT,
      ipaddress STRING,
      operation STRING,
      domain STRING,
      objct STRING,
      httpstatus STRING,
      referrer STRING, 
      useragent STRING,
      querystring STRING
    )
    ...

## Usage

You can download a jarfile for cloudfront-log-deserializer from GitHub from the [Downloads] [downloads] menu.

Once you have the jarfile on your classpath, using this Deserializer with Hive is easy:

    TODO 

Note that in the `CREATE TABLE` statement above, you do **not** have to manually specify all of the columns to create for this table. This is because Hive will query the SerDe to determine the _actual_ list of columns for this table.

## See also

If you find this Deserializer helpful, you might also want to take a look at:

* Amazon's own [CloudFront log analyzer] [loganalyzer] for Hadoop 
* The [S3LogDeserializer] [s3logdeserializer] which comes bundled with Hive

## Copyright and license

cloudfront-log-deserializer is copyright 2012 Orderly Ltd.

Licensed under the [Apache License, Version 2.0] [license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[hive]: http://hive.apache.org/ 
[snowplowserdes]: https://github.com/snowplow/snowplow/tree/master/serdes
[awslogdocs]: http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/AccessLogs.html#LogFileFormat
[license]: http://www.apache.org/licenses/LICENSE-2.0
[loganalyzer]: http://elasticmapreduce.s3.amazonaws.com/samples/cloudfront/code/cloudfront-loganalyzer.tgz
[w3cformat]: http://www.w3.org/TR/WD-logfile.html 
[s3logdeserializer]: http://javasourcecode.org/html/open-source/hive/hive-0.7.1/org/apache/hadoop/hive/contrib/serde2/s3/S3LogDeserializer.html
[downloads]: https://github.com/snowplow/cloudfront-log-deserializer/downloads
