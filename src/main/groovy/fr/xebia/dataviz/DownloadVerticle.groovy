package fr.xebia.dataviz

import groovyx.net.http.HTTPBuilder
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal

/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:29
 */
class DownloadVerticle extends Verticle {

    def start() {

        def mapInsee = [:]
        def cities = []


        def httpOpenMap = new HTTPBuilder('http://nominatim.openstreetmap.org')

        findOnDisk("insee.csv").splitEachLine(";") { fields ->


            mapInsee.put(fields[3].padLeft(5, "0"), [
                    code: fields[3],
                    postcode: fields[1],
                    formattedName: fields[0],
                    region: fields[2]
            ])
        }


        new File("out.txt").withWriter { out ->
            findOnDisk("HIST_POP_COM_RP10.csv").splitEachLine(";") { fields ->

                def insee = mapInsee.get(fields[0])
                if (insee != null) {
                    def city = [
                            code: insee.code,
                            postcode: insee.postcode,
                            region: insee.region,
                            formattedName: insee.formattedName,
                            prettyName: fields[1],
                            country: "France",
                            countryCode: "fr",
                            population: fields[2]
                    ]

//                    println "Accessing path : /nominatim/v1/search?format=json&addressdetails=1&polygon=1&countrycodes=${city.countryCode}&q=${city.formattedName},${city.region}"

                    httpOpenMap.get(path: '/search', query: [
                            format: "json",
                            addressdetails: "0",
                            polygon: "1",
                            countrycodes: city.countryCode,
                            email: "mboudraa@xebia.fr",
                            q: "${city.prettyName},${city.region}"

                    ]) { resp, json ->
                        json.find { it ->
                            if (it.type == "administrative") {
                                city.put("latlng", [lat: it.lat, lng: it.lon])
                                city.put("boundingbox", it.boundingbox)
                                city.put("boundingbox", it.boundingbox)
                                city.put("polygonpoints", it.polygonpoints)

                                cities.add(city)
                                println cities.size()

                                return true
                            }
                            return false
                        }

                    }


                } else {
                    out.writeLine fields.toString()
                }
            }
        }


        println "${cities.size()} registered cities "

    }

    private File findOnDisk(String resourceRelativePath) {
        VertxInternal core = vertx.toJavaVertx() as VertxInternal
        String pathToDisk = PathAdjuster.adjust(core, resourceRelativePath)
        new File(pathToDisk)
    }

    private String getUri(inseeCode) {
        return "/demo/populationLegale/commune/${inseeCode}/2010"
    }

}

