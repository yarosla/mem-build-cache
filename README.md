Gradle Build Cache Server
=========================

Remote in-memory cache to speed up Gradle build process.
See details [here](https://docs.gradle.org/current/userguide/build_cache.html).

Quick start
-----------

    gradle clean fatJar
    java -jar build/libs/mem-build-cache-1.0.jar -u build-cache-user -p some-password

Add to your `settings.gradle`:

    buildCache {
        remote(HttpBuildCache) {
            url = 'http://localhost:8080/cache/'
            push = true
            credentials {
                username = 'build-cache-user'
                password = 'some-password'
            }
        }
    }

Then run your project's Gradle build:

    gradle --build-cache ...

Options
-------

    --debug, -v
      Show debug log
      Default: false
    --help, -h
      Display help
    --host, -H
      Set http host to listen on
      Default: 0.0.0.0
    --limit, -l
      Memory limit in megabytes
      Default: 1000
    --password, -p
      Set password for authenticated access
    --port, -P
      Set http port to listen to
      Default: 8080
    --user, -u
      Set username for authenticated access

Statistics
----------

Inspect cache statistics (in JSON format):

    curl -u build-cache-user:some-password http://localhost:8080/stats

Technologies
------------

- Spring 5 WebFlux + Netty
- Caffeine Cache

Author
------

Yaroslav Stavnichiy <yarosla@gmail.com>
