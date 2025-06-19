
# bank-account-verification-example-frontend

`bank-account-verification-example-frontend` (`BAVEFE`) is an example client service of [bank-account-verification-frontend](https://github.com/hmrc/bank-account-verification-frontend).

`bank-account-verification-frontend` API usage is decribed in its [README](https://github.com/hmrc/bank-account-verification-frontend)

`bank-account-verification-acceptance-example` is our recommended solution for creating acceptance tests for a service that integrates with `bank-account-verification-frontend`.  Check it out on [Github](https://github.com/hmrc/bank-account-verification-acceptance-example).

# Running locally

Start the services via service manager:

```sm2 --start BANK_ACCOUNT_VERIFICATION```

Start this frontend service:

```sbt run```

In your browser, navigate to http://localhost:9929/bank-account-verification-example-frontend.




### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
