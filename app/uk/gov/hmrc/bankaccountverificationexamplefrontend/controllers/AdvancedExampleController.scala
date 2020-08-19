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
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html._
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{BavfConnector, InitRequestMessages}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AdvancedExampleController @Inject()(appConfig: AppConfig,
                                          connector: BavfConnector,
                                          mcc: MessagesControllerComponents,
                                          headerBlock: HeaderBlock,
                                          beforeContentBlock: BeforeContentBlock,
                                          footerBlock: FooterBlock,
                                          startPage: StartPage,
                                          donePage: DonePage)
  extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val start: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(startPage(
      action = uk.gov.hmrc.bankaccountverificationexamplefrontend.controllers.routes.AdvancedExampleController.transfer,
      beforeContentBlock = Some(beforeContentBlock()))))
  }

  val transfer: Action[AnyContent] = Action.async { implicit request =>
    val continueUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/advanced/done"

    connector.init(continueUrl, Some(headerBlock()), Some(beforeContentBlock()), Some(footerBlock()), requestMessages).map {
      case Some(journeyId) =>
        val redirectUrl = s"${appConfig.bavfWebBaseUrl}/bank-account-verification/start/$journeyId"
        SeeOther(redirectUrl)
      case None =>
        InternalServerError
    }
  }

  def done(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    connector.complete(journeyId).map {
      case Some(r) => Ok(donePage(r,
        beforeContentBlock = Some(beforeContentBlock())))
      case None => InternalServerError
    }
  }

  private def accessibilityUrl(implicit messages: Messages) =
    s"${appConfig.exampleExternalUrl}${messages("footer.accessibility.url")}"

  private def requestMessages(implicit messages: Messages) = {
    Some(InitRequestMessages(Json.obj(
      "service.name" -> messages("service.name"),
      "service.header" -> messages("bavf.service.header"),
      "footer.accessibility.url" -> s"${appConfig.exampleExternalUrl}${messages("footer.accessibility.url")}"
    )))
  }
}
