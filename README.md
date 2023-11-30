#Red Hat Insights for JWS

`mvn package` to build.
The listener should be added to `server.xml`, such as
```
<Server>
  <Listener className="com.redhat.jws.insights.InsightsLifecycleListener" />
</Server>
```

Configuration is done through the standard `com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration` from Insights, which uses various environment and Java system properties. Alternatly, attributes matching the properties can be set on the `Listener` element in `server.xml`:
`identificationName`, `machineIdFilePath`, `archiveUploadDir`, `certFilePath`, `certHelperBinary`, `connectPeriod`,
 `httpClientRetryBackoffFactor`, `httpClientRetryInitialDelay`, `httpClientRetryMaxAttempts`, `httpClientTimeout`, `keyFilePath`,
 `maybeAuthToken`, `proxyHost`, `proxyPort`, `updatePeriod`, `uploadBaseURL`, `uploadUri`, `optingOut`.
