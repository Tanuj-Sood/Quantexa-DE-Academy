package com.quantexa.assessments.scoringModel

import com.quantexa.assessments.accounts.AccountAssessment.AccountData
import com.quantexa.assessments.customerAddresses.CustomerAddress.AddressData
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{Dataset, SparkSession}

/***
  * Part of the Quantexa solution is to flag high risk countries as a link to these countries may be an indication of
  * tax evasion.
  *
  * For this question you are required to populate the flag in the ScoringModel case class where the customer has an
  * address in the British Virgin Islands.
  *
  * This flag must be then used to return the number of customers in the dataset that have a link to a British Virgin
  * Islands address.
  */

object ScoringModel extends App {


  //Create a spark context, using a local master so Spark runs on the local machine
  val spark = SparkSession.builder().master("local[*]").appName("ScoringModel").getOrCreate()

  //importing spark implicits allows functions such as dataframe.as[T]
  import spark.implicits._

  //Set logger level to Warn
  Logger.getRootLogger.setLevel(Level.WARN)

  case class CustomerDocument(
                               customerId: String,
                               forename: String,
                               surname: String,
                               //Accounts for this customer
                               accounts: Seq[AccountData],
                               //Addresses for this customer
                               address: Seq[AddressData]
                             )

  case class ScoringModel(
                           customerId: String,
                           forename: String,
                           surname: String,
                           //Accounts for this customer
                           accounts: Seq[AccountData],
                           //Addresses for this customer
                           address: Seq[AddressData],
                           linkToBVI: Boolean
                         )

  val customerDocumentDS: Dataset[CustomerDocument] =
    spark.read.parquet("src/main/resources/customerDocument.parquet").as[CustomerDocument]

  val scoringModelDS: Dataset[ScoringModel] =
    customerDocumentDS
      .map { customerDocument =>
        val linkToBVI = customerDocument.address.exists { addressData =>
          addressData.country.exists(country => country == "British Virgin Islands")
        }

        ScoringModel(
          customerId = customerDocument.customerId,
          forename = customerDocument.forename,
          surname = customerDocument.surname,
          accounts = customerDocument.accounts,
          address = customerDocument.address,
          linkToBVI = linkToBVI
        )
      }

  val linkedToBVICustomerCount = scoringModelDS.filter(scoringModel => scoringModel.linkToBVI).count()

  scoringModelDS.show(1000,false)
  println(s"Number of customers linked to British Virgin Islands: $linkedToBVICustomerCount")

  //END GIVEN CODE

}
