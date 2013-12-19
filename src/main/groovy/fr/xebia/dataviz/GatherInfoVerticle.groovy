package fr.xebia.dataviz

import groovyx.net.http.HTTPBuilder
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

/**
 * Created with IntelliJ IDEA.
 * User: pablolopez
 * Date: 13/12/13
 * Time: 09:36
 * To change this template use File | Settings | File Templates.
 */
class GatherInfoVerticle extends Verticle {

    HTTPBuilder httpOpenMap;

    def start() {

        vertx.eventBus.registerHandler('fr.xebia.dataviz.gatherInfo', this.&gatherInfo)
        httpOpenMap = new HTTPBuilder('http://open.mapquestapi.com')

    }

    def gatherInfo(Message message){
        def insee = message.body.insee
        def prettyName = message.body.prettyName
        def population = message.body.population

        def start1 = System.currentTimeMillis()

        if (insee != null) {
            def city = [
                    code: insee.code,
                    postcode: insee.postcode,
                    region: insee.region,
                    formattedName: insee.formattedName,
                    prettyName: prettyName,
                    countryCode: "fr",
                    population: population
            ]

            try {
                httpOpenMap.get(path: '/nominatim/v1/search', query: [
                        format: "json",
                        addressdetails: "1",
                        polygon: "1",
                        countrycodes: city.countryCode,
                        email: "mboudraa@xebia.fr",
                        q: "${city.prettyName},${city.region}"

                ]) { resp, json ->
                    json.find { it ->
                        if (it['type'] == "administrative") {
                            city.put("latlng", [lat: it.lat, lng: it.lon])
                            city.put("boundingbox", it.boundingbox)
                            city.put("polygonpoints", it.polygonpoints)
                            city.put("country", it.address.country)

                            def end1 = System.currentTimeMillis()
                            println " * ${(end1 - start1)} ms - ${city.prettyName} (${city.postcode}) ==> ${it.address}"

                            sendToElasticSearch(city)


                            return true
                        }
                        return false
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void sendToElasticSearch(city) {

        Map message = [
                "index": "cities",
                "entity": "ville_fr",
                "id": city.code,
                "content": city
        ]



        vertx.eventBus.send("fr.xebia.dataviz.es.createObject", message) { response ->
            println response
        }
    }
}
