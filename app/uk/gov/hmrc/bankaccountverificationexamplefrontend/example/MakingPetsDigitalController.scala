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

package uk.gov.hmrc.bankaccountverificationexamplefrontend.example

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.example.html.PetDetails
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{BusinessDonePage, PersonalDonePage}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{AuthProviderId, BavfConnector, InitRequestMessages, InitRequestPrepopulatedData}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MakingPetsDigitalController @Inject()(appConfig: AppConfig,
                                            connector: BavfConnector,
                                            val authConnector: AuthConnector,
                                            mcc: MessagesControllerComponents,
                                            petDetails: PetDetails,
                                            personalDonePage: PersonalDonePage,
                                            businessDonePage: BusinessDonePage)
  extends FrontendController(mcc) with AuthorisedFunctions {

  implicit val config: AppConfig = appConfig

  val getDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Ok(petDetails(PetDetailsRequest.form)))
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  val postDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) {
      authProviderId =>
        val form = PetDetailsRequest.form.bindFromRequest()

        if (form.hasErrors)
          Future.successful(BadRequest(petDetails(form)))
        else {
          val continueUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/done"
          val requestData = form.value.get
          val initRequestPrepopulatedAccountInformation = InitRequestPrepopulatedData.from(
            accountType = requestData.accountType,
            name = requestData.accountName,
            sortCode = requestData.sortCode,
            accountNumber = requestData.accountNumber,
            rollNumber = requestData.rollNumber)

          connector.init(continueUrl, messages = requestMessages, prepopulatedData = initRequestPrepopulatedAccountInformation).map {
            case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
            case None => InternalServerError
          }
        }
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
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