/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bankaccountverificationexamplefrontend.views

import play.api.i18n.Messages
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{BusinessCompleteResponse, PersonalCompleteResponse}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryListRow}

import java.net.URLEncoder

object TemplateHelper {

  def fixRows(response: PersonalCompleteResponse, rows: Seq[SummaryListRow], accountType: String, changeUrl: String)(implicit messages: Messages):Seq[SummaryListRow] = {
    val queryMap: Map[String, String] = Map("accountType" -> accountType, "accountName" -> response.accountName, "sortCode" -> response.sortCode, "accountNumber" -> response.accountNumber) ++ response.rollNumber.fold(Map[String, String]())(rn => Map("rollNumber" -> rn))

    doFixRows(rows, changeUrl, queryMap)
  }

  def fixRows(response: BusinessCompleteResponse, rows: Seq[SummaryListRow], accountType: String, changeUrl: String)(implicit messages: Messages):Seq[SummaryListRow] = {
    val queryMap: Map[String, String] = Map("accountType" -> accountType, "companyName" -> response.companyName, "sortCode" -> response.sortCode, "accountNumber" -> response.accountNumber) ++ response.rollNumber.fold(Map[String, String]())(rn => Map("rollNumber" -> rn))

    doFixRows(rows, changeUrl, queryMap)
  }

  private def doFixRows(rows: Seq[SummaryListRow], changeUrl: String, queryMap: Map[String, String])(implicit messages: Messages) = {
    val queryString: String = queryMap.map{case(k, v) => s"""$k=${URLEncoder.encode(v, "utf-8")}"""}.mkString("?", "&", "")

    rows.collect {
      case r if r == rows.head => r.copy(classes = r.classes + " account-details-summary-row--first")
      case r if r == rows.last => r.copy(
        classes = r.classes.replace("govuk-summary-list__row--no-border", "")
            + " account-details-summary-row--last",
        actions = Some(Actions(items = Seq(
          ActionItem(
            href = s"$changeUrl$queryString",
            content = HtmlContent(
              s"""
                  <span aria-hidden="true">${messages("label.change")}</span>
                  <span class="govuk-visually-hidden">${messages("label.change")} ${messages("label.accountDetails.heading")}</span>
               """
            )
          )))))
      case r                   => r
    }
  }
}