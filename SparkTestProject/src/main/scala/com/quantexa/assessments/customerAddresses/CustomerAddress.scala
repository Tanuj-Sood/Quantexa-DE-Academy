package com.quantexa.assessments.customerAddresses

import com.quantexa.assessments.accounts.AccountAssessment.{AccountData, CustomerAccountOutput}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

/***
  * A problem we have at Quantexa is where an address is populated with one string of text. In order to use this information
  * in the Quantexa product, this field must be "parsed".
  *
  * The purpose of parsing is to extract information from an entry - for example, to extract a forename and surname from
  * a 'full_name' field. We will normally place the extracted fields in a case class which will sit as an entry in the
  * wider case class; parsing will populate this entry.
  *
  * The parser on an address might yield the following "before" and "after":
  *
  * +-----------------------------------------+-------+--------------------+-------+--------+
  * |Address                                  |number |road                |city   |country |
  * +-----------------------------------------+-------+--------------------+-------+--------+
  * |109 Borough High Street, London, England |null   |null                |null   |null    |
  * +-----------------------------------------+-------+--------------------+-------+--------+
  *
  * +-----------------------------------------+-------+--------------------+-------+--------+
  * |Address                                  |number |road                |city   |country |
  * +-----------------------------------------+-------+--------------------+-------+--------+
  * |109 Borough High Street, London, England |109    |Borough High Street |London |England |
  * +-----------------------------------------+-------+--------------------+-------+--------+
  *
  *
  * You have been given addressData. This has been read into a DataFrame for you and then converted into a
  * Dataset of the given raw case class.
  *
  * You have been provided with a basic address parser which must be applied to the CustomerDocument model.
  *

  * Example Answer Format:
  *
  * val customerDocument: Dataset[CustomerDocument] = ???
  * customerDocument.show(1000,truncate = false)
  *
  * +----------+-----------+-------+--------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
  * |customerId|forename   |surname|accounts                                                            |address                                                                                                                               |
  * +----------+-----------+-------+--------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------+
  * |IND0001   |Christopher|Black  |[]                                                                  |[[ADR360,IND0001,762, East 14th Street, New York, United States of America,762, East 14th Street, New York, United States of America]]|
  * |IND0002   |Madeleine  |Kerr   |[[IND0002,ACC0155,323], [IND0002,ACC0262,60]]                       |[[ADR139,IND0002,675, Khao San Road, Bangkok, Thailand,675, Khao San Road, Bangkok, Thailand]]                                        |
  * |IND0003   |Sarah      |Skinner|[[IND0003,ACC0235,631], [IND0003,ACC0486,400], [IND0003,ACC0540,53]]|[[ADR318,IND0003,973, Blue Jays Way, Toronto, Canada,973, Blue Jays Way, Toronto, Canada]]                                            |
  * | ...
  */

object CustomerAddress extends App {

  //Create a spark context, using a local master so Spark runs on the local machine
  val spark = SparkSession.builder().master("local[*]").appName("CustomerAddress").getOrCreate()

  //importing spark implicits allows functions such as dataframe.as[T]

  import spark.implicits._

  //Set logger level to Warn
  Logger.getRootLogger.setLevel(Level.WARN)

  val outputBasePath =
    sys.props.get("qde.output.base.path")
      .orElse(sys.env.get("QDE_OUTPUT_BASE_PATH"))
      .getOrElse("src/main/resources")

  case class AddressRawData(
                             addressId: String,
                             customerId: String,
                             address: String
                           )

  case class AddressData(
                          addressId: String,
                          customerId: String,
                          address: String,
                          number: Option[Int],
                          road: Option[String],
                          city: Option[String],
                          country: Option[String]
                        )

  //Expected Output Format
  case class CustomerDocument(
                               customerId: String,
                               forename: String,
                               surname: String,
                               //Accounts for this customer
                               accounts: Seq[AccountData],
                               //Addresses for this customer
                               address: Seq[AddressData]
                             )


  def addressParser(unparsedAddress: Seq[AddressData]): Seq[AddressData] = {
    unparsedAddress.map(address => {
      val split = address.address.split(", ")

      address.copy(
        number = Some(split(0).toInt),
        road = Some(split(1)),
        city = Some(split(2)),
        country = Some(split(3))
      )
    }
    )
  }


  val addressDF: DataFrame = spark.read.option("header", "true").csv(s"$outputBasePath/address_data.csv")

  val customerAccountOutputPath = s"$outputBasePath/customerAccountOutputDS.parquet"
  val customerAccountOutputDirectory = new java.io.File(customerAccountOutputPath)
  val customerAccountOutputFiles = Option(customerAccountOutputDirectory.listFiles()).getOrElse(Array.empty)
  val hasCustomerAccountParquetData =
    customerAccountOutputDirectory.exists() && customerAccountOutputFiles.exists(file => file.getName.startsWith("part-"))

  require(
    hasCustomerAccountParquetData,
    s"Expected parquet output at $customerAccountOutputPath. Run AccountAssessment first to generate customerAccountOutputDS.parquet"
  )

  val customerAccountDS = spark.read.parquet(customerAccountOutputPath).as[CustomerAccountOutput]

  val addressRawDS: Dataset[AddressRawData] = addressDF.as[AddressRawData]

  val groupedAddressByCustomer: Dataset[(String, Seq[AddressData])] =
    addressRawDS
      .map { addressRaw =>
        AddressData(
          addressId = addressRaw.addressId,
          customerId = addressRaw.customerId,
          address = addressRaw.address,
          number = None,
          road = None,
          city = None,
          country = None
        )
      }
      .groupByKey(address => address.customerId)
      .mapGroups {
        case (customerId, groupedAddress) =>
          customerId -> groupedAddress.toSeq
      }

  val customerDocument: Dataset[CustomerDocument] =
    customerAccountDS
      .joinWith(
        groupedAddressByCustomer,
        customerAccountDS("customerId") === groupedAddressByCustomer("_1"),
        "left_outer"
      )
      .map {
        case (customerAccount, groupedAddress) =>
          val unparsedAddress = Option(groupedAddress).map(_._2).getOrElse(Seq.empty[AddressData])

          CustomerDocument(
            customerId = customerAccount.customerId,
            forename = customerAccount.forename,
            surname = customerAccount.surname,
            accounts = customerAccount.accounts,
            address = addressParser(unparsedAddress)
          )
      }

  customerDocument.show(1000, truncate = false)
  customerDocument.write.mode("overwrite").parquet(s"$outputBasePath/customerDocument.parquet")

  //END GIVEN CODE


}
