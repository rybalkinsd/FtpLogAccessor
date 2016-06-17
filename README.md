# FRAG missions log accessor

This application downloads many small (up to 30 Mb) log files from many FTP servers.

FTP servers hosts are defined in `server.properties` using `server.mask` and `server.number`.

*Setup*

1. Add `credentials.properties` file with `server.login` and `server.password`. All servers supposed to have the same credentials. If no - just change `serversList` bean configuration.
2. Run `gradle :clean :jar`.
3. Run `java -jar FtpLogAccessor-[version].jar`
