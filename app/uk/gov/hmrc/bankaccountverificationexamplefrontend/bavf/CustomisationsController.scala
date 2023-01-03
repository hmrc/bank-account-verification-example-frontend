/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{BeforeContentBlock, FooterBlock, HeaderBlock}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class CustomisationsController @Inject()(appConfig: AppConfig,
                                         mcc: MessagesControllerComponents,
                                         headerBlock: HeaderBlock,
                                         beforeContentBlock: BeforeContentBlock,
                                         footerBlock: FooterBlock)
  extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val header: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(headerBlock()))
  }

  val beforeContent: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(beforeContentBlock()))
  }

  val footer: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(footerBlock()))
  }
}