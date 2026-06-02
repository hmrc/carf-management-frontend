
# carf-management-frontend

This is the Frontend repository for the Crypto Asset Reporting Framework (CARF) team's management journey

## What this service does

This service allows registered users to add RCASPs (Reporting Crypto Asset Service Providers) that they will later submit data for

### Running the service locally

Prerequisites:
- Java 21
- SBT
- MongoDB
- Service Manager
- Node Version manager (nvm)
- NodeJs

Commands:

Start CARF services in service manager. (frontend,backend, any other services needed to run locally)

```
sm2 --start CARF_ALL
```
Stop this service from service manager.

```
sm2 --stop CARF_MANAGEMENT_FRONTEND 
```
Run CARF_MANAGEMENT_FRONTEND locally using sbt to test any non-merged changes with:

```
sbt run
```

### Service manager and port info

Service manager: CARF_ALL

Port: 17002

### How to test a journey locally and on staging

Local:
http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:17002/rcasp/manage-cryptoasset-reports 

Staging:
https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in?continue=%2Frcasp%2Fmanage-cryptoasset-reports

In both cases, a user must have a carf registration.

To add this, you must scroll down to the enrolments section and add the following:
Enrolment Key: HMRC-CARF-ORG
Identifier Name: CARFID
Identifier Value: 1111

To test different starting parameters, please refer to the carf testing area on our confluence page, or our stubs repository

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").