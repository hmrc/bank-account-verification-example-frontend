/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountverificationexamplefrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.language.{Cy, En, Language}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  val footerLinkItems: Seq[String] = config.getOptional[Seq[String]]("footerLinkItems").getOrElse(Seq())

  val bavfApiBaseUrl = servicesConfig.baseUrl("bank-account-verification-api")
  val bavfWebBaseUrl = servicesConfig.baseUrl("bank-account-verification-web")

  val exampleExternalUrl = servicesConfig.baseUrl("bank-account-verification-example-frontend.external")
  val exampleInternalUrl = servicesConfig.baseUrl("bank-account-verification-example-frontend.internal")

  val authLoginStubPath = servicesConfig.getConfString("auth-login-stub.path","")
  val authLoginStubUrl = servicesConfig.baseUrl("auth-login-stub") +
    authLoginStubPath + "?continue=" +
    exampleExternalUrl + "/bank-account-verification-example-frontend" +
    uk.gov.hmrc.bankaccountverificationexamplefrontend.example.routes.MakingPetsDigitalController.getDetails().url

  def langToLanguage(langCode: String): Language = langCode match {
    case "en" ⇒ En
    case "cy" ⇒ Cy
    case _ => En
  }
}
