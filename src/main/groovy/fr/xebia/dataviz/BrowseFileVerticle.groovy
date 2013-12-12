package fr.xebia.dataviz
import com.englishtown.vertx.elasticsearch.ElasticSearch
import groovyx.net.http.HTTPBuilder
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal
import org.vertx.java.core.json.impl.Json
/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:29
 */
class BrowseFileVerticle extends Verticle {

    def start() {

        def mapInsee = [:]
        def cities = []


        def start = System.currentTimeMillis()

        findOnDisk("insee.csv").splitEachLine(";") { fields ->


            mapInsee.put(fields[3].padLeft(5, "0"), [
                    code: fields[3],
                    postcode: fields[1],
                    formattedName: fields[0],
                    region: fields[2]
            ])
        }

        def httpOpenMap = new HTTPBuilder('http://open.mapquestapi.com')

        new File("out.txt").withWriter { out ->
            findOnDisk("HIST_POP_COM_RP10.csv").splitEachLine(";") { fields ->

                def start1 = System.currentTimeMillis()

                def insee = mapInsee.get(fields[0])
                if (insee != null) {
                    def city = [
                            code: insee.code,
                            postcode: insee.postcode,
                            region: insee.region,
                            formattedName: insee.formattedName,
                            prettyName: fields[1],
                            countryCode: "fr",
                            population: fields[2]
                    ]

//                    println "Accessing path : /nominatim/v1/search?format=json&addressdetails=1&polygon=1&countrycodes=${city.countryCode}&q=${city.formattedName},${city.region}"

                    httpOpenMap.get(path: '/nominatim/v1/search', query: [
                            format: "json",
                            addressdetails: "1",
                            polygon: "1",
                            countrycodes: city.countryCode,
                            email: "mboudraa@xebia.fr",
                            q: "${city.prettyName},${city.region}"

                    ]) { resp, json ->
                        json.find { it ->
                            if (it.hasProperty('type') == true && it.type == "administrative") {
                                city.put("latlng", [lat: it.lat, lng: it.lon])
                                city.put("boundingbox", it.boundingbox)
                                city.put("polygonpoints", it.polygonpoints)
                                city.put("country", it.address.country)

                                cities.add(city)

                                def end1 = System.currentTimeMillis()
                                println " *  ${cities.size()} - ${(end1 - start1)} ms - ${city.prettyName} (${city.postcode}) ==> ${it.address}"

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



        def end = System.currentTimeMillis()

        println "SUCCESSFUL => ${(end - start) / 6000} minutes"
        println "${cities.size()} registered cities "

    }

    private void sendToElasticSearch(city) {
        def cityJson = Json.encode(city)

        Map message = [
                "address": "eb.elasticsearch",
                "transportAddresses": [["hostname": "host1", "port": 9300], ["hostname": "host2", "port": 9301]],
                "cluster_name": "my_cluster",
                "client_transport_sniff": true,
                "action": "index",
                "${ElasticSearch.CONST_INDEX}": "cities",
                "${ElasticSearch.CONST_TYPE}": "ville_fr",
                "${ElasticSearch.CONST_SOURCE}": cityJson,

        ]


        vertx.eventBus().send(ElasticSearch.DEFAULT_ADDRESS, message, new Handler<Message<Map>>() {
            @Override
            public void handle(Message<Map> reply) {
                Map body = reply.body()
                println body
            }
        });
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

