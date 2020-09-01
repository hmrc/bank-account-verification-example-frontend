/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountverificationexamplefrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{
  BavfConnector,
  InitRequestMessages
}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{
  PersonalDonePage,
  BusinessDonePage,
  StartPage
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ExampleController @Inject()(appConfig: AppConfig,
                                  connector: BavfConnector,
                                  mcc: MessagesControllerComponents,
                                  startPage: StartPage,
                                  personalDonePage: PersonalDonePage,
                                  businessDonePage: BusinessDonePage)
    extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val start: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(
        startPage(
          action =
            uk.gov.hmrc.bankaccountverificationexamplefrontend.controllers.routes.ExampleController.transfer
        )
      )
    )
  }

  val transfer: Action[AnyContent] = Action.async { implicit request =>
    val continueUrl =
      s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/done"

    connector.init(continueUrl, messages = requestMessages).map {
      case Some(journeyId) =>
        val redirectUrl =
          s"${appConfig.bavfWebBaseUrl}/bank-account-verification/start/$journeyId"
        SeeOther(redirectUrl)
      case None =>
        InternalServerError
    }
  }

  def done(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      connector.complete(journeyId).map {
        case Some(r) if r.accountType == "personal" =>
          Ok(personalDonePage(r.personal.get))
        case Some(r) if r.accountType == "business" =>
          Ok(businessDonePage(r.business.get))
        case None => InternalServerError
      }
  }

  private def requestMessages(implicit messagesApi: MessagesApi) = {
    val english = messagesApi.preferred(Seq(Lang("en")))
    val welsh = messagesApi.preferred(Seq(Lang("cy")))
    Some(
      InitRequestMessages(
        en = Json.obj(
          "service.name" -> english("service.name"),
          "footer.accessibility.url" -> s"${appConfig.exampleExternalUrl}${english("footer.accessibility.url")}",
          "phaseBanner.tag" -> "BETA"
        ),
        cy = Some(
          Json.obj(
            "service.name" -> welsh("service.name"),
            "footer.accessibility.url" -> s"${appConfig.exampleExternalUrl}${welsh("footer.accessibility.url")}",
            "phaseBanner.tag" -> "BETA"
          )
        )
      )
    )
  }
}
