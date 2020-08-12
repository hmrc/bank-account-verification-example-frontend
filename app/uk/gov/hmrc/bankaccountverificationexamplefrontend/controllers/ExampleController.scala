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
import play.api.mvc._
import uk.gov.hmrc.bankaccountverificationexamplefrontend.BavfConnector
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.ExamplePage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ExampleController @Inject()(appConfig: AppConfig,
                                  connector: BavfConnector,
                                  mcc: MessagesControllerComponents,
                                  examplePage: ExamplePage)
  extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val example: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(examplePage()))
  }

  val transfer: Action[AnyContent] = Action.async { implicit request =>
    connector.init.map {
      case Some(journeyId) =>
        val redirectUrl = s"${appConfig.bavfBaseUrl}/bank-account-verification/start/$journeyId"
        SeeOther(redirectUrl)
      case None =>
        InternalServerError
    }
  }

}
