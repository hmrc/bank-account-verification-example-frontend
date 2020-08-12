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

package uk.gov.hmrc.bankaccountverificationexamplefrontend.bavf

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Messages, MessagesProvider}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.i18n.MessagesApi
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{FooterBlock, HeaderBlock}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future
import scala.io.Source

@Singleton
class CustomisationsController @Inject()(appConfig: AppConfig,
                                         mcc: MessagesControllerComponents,
                                         headerBlock: HeaderBlock,
                                         footerBlock: FooterBlock)
  extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val header: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(headerBlock()))
  }

  val footer: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(footerBlock()))
  }

  val messages: Action[AnyContent] = Action.async { implicit request =>
    val source = Source.fromInputStream(getClass.getResourceAsStream("/messages"))
    val text = try source.mkString finally source.close()

    Future.successful(Ok(text))
  }
}
