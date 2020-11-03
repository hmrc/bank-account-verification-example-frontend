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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.BavfConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.example.MakingPetsDigitalController
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.StartPage
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{BusinessDonePage, PersonalDonePage}

class MakingPetsDigitalControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  private val fakeRequest = FakeRequest("GET", "/")

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  val startPage: StartPage = app.injector.instanceOf[StartPage]
  val personalDonePage: PersonalDonePage = app.injector.instanceOf[PersonalDonePage]
  val businessDonePage: BusinessDonePage = app.injector.instanceOf[BusinessDonePage]
  val connector: BavfConnector = app.injector.instanceOf[BavfConnector]

  private val controller = new MakingPetsDigitalController(appConfig, connector, stubMessagesControllerComponents(), startPage, personalDonePage, businessDonePage)

  "GET /" should {
    "return 200" in {
      val result = controller.start(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = controller.start(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }
  }
}