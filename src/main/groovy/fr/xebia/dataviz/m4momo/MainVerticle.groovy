package fr.xebia.dataviz.m4momo

import org.vertx.groovy.core.http.HttpClient
import org.vertx.groovy.core.http.HttpClientResponse
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal
import org.vertx.java.core.json.impl.Json

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by amaury on 12/12/2013.
 */
class MainVerticle extends Verticle {

    String TARGET = 'open.mapquestapi.com'
    String TARGET_PATH = '/nominatim/v1/search'

    HttpClient httpClient

    def inseeMap

    AtomicInteger inputLines = new AtomicInteger(0)
    AtomicInteger notAdministrativeCities = new AtomicInteger(0)
    AtomicInteger administrativeCities = new AtomicInteger(0)
    AtomicInteger launchedRequests = new AtomicInteger(0)
    AtomicInteger respondedRequests = new AtomicInteger(0)
    AtomicInteger errorsInBody = new AtomicInteger(0)
    AtomicInteger byPass = new AtomicInteger(0)

    def loadInseeData() {

        inseeMap = [:] as ConcurrentHashMap

        httpClient = vertx.createHttpClient(port: 80, host: TARGET, maxPoolSize: 1)

        findOnDisk('insee.csv').splitEachLine(';') { fields ->
            String key = fields[3].padLeft(5, '0')

            inseeMap[key] = [
                    code: fields[3],
                    postcode: fields[1],
                    formattedName: fields[0],
                    region: fields[2]
            ]
        }
    }


    def start() {

        loadInseeData()

        println "DONE - Insee file load ."

        def cities = []

        vertx.setPeriodic(1000) { timerId ->
            println "READ: ${inputLines.get()} - BYPASS: ${byPass.get()} - REQ: ${launchedRequests.get()} - RESP: ${respondedRequests.get()} - ERR: ${errorsInBody.get()}"
            println "ADMIN: ${administrativeCities.get()} - NOT: ${notAdministrativeCities}"
        }

        new File('out.txt').withWriter { out ->

            findOnDisk("HIST_POP_COM_RP10.csv").splitEachLine(";") { fields ->
                inputLines.incrementAndGet()

                def insee = inseeMap[fields[0]]

                if (insee) {
                    def city = [
                            code: insee.code,
                            postcode: insee.postcode,
                            region: insee.region,
                            formattedName: insee.formattedName,
                            prettyName: fields[1],
                            country: 'France',
                            countryCode: 'fr',
                            population: fields[2]
                    ]

                    def queryParams = [
                            format: 'json',
                            addressdetails: '1',
                            polygon: '1',
                            countrycodes: city.countryCode,
                            email: 'mboudraa@xebia.fr',
                            q: "${city.prettyName},${city.region}"

                    ].collect { key, value -> key + '=' + value }.join('&')

                    // println "GET ${TARGET}${TARGET_PATH}?${queryParams}"
                    launchedRequests.incrementAndGet()
                    httpClient.getNow(TARGET_PATH+ '?' + queryParams) { HttpClientResponse response ->
                        respondedRequests.incrementAndGet()
                        response.bodyHandler { body ->

                            try {
                                def parsed = Json.decodeValue(body.toString(), ArrayList.class)

                                parsed.find { it ->
                                    if ('administrative' == it.type) {
                                        administrativeCities.incrementAndGet()
                                        city.latlng = [lat: it.lat, lng: it.lon]
                                        city.boundingbox = it.boundingbox
                                        city.country = it.address.country
                                        city.polygonpoints = it.polygonpoints

                                        cities.add(city)
                                    } else {
                                        notAdministrativeCities.incrementAndGet()
                                    }
                                }
                            } catch (Exception e) {
                                println "GET ${TARGET}${TARGET_PATH}?${queryParams}"
                                println e.message
                                println body
                                errorsInBody.incrementAndGet()
                            }
                        }
                    }
                } else {
                    byPass.incrementAndGet()
                    out.writeLine fields.toString()
                }
            }
        }
    }

    private File findOnDisk(String resourceRelativePath) {
        VertxInternal core = vertx.toJavaVertx() as VertxInternal
        String pathToDisk = PathAdjuster.adjust(core, resourceRelativePath)
        new File(pathToDisk)
    }
}
