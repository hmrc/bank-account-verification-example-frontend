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

package uk.gov.hmrc.bankaccountverificationexamplefrontend

import javax.inject.Inject
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class BavfConnector @Inject()(httpClient: HttpClient, appConfig: AppConfig) {

  def init(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[String]] = {
    import HttpReads.Implicits.readRaw

    val url = s"${appConfig.bavfBaseUrl}/api/init"
    httpClient.POSTEmpty[HttpResponse](url).map {
      case r if r.status == 200 =>
        Some(r.json.as[String])
      case _ =>
        None
    }
  }

}
