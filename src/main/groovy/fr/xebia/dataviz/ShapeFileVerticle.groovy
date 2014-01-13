package fr.xebia.dataviz

import com.vividsolutions.jts.geom.Geometry
import org.geotools.data.FeatureSource
import org.geotools.data.FileDataStore
import org.geotools.data.FileDataStoreFinder
import org.geotools.feature.FeatureCollection
import org.geotools.feature.FeatureIterator
import org.opengis.feature.simple.SimpleFeature
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal

/**
 * User: mounirboudraa
 * Date: 13/12/2013
 * Time: 17:25
 */
class ShapeFileVerticle extends Verticle {

    int count = 0
    int total = 0

    def map = [:]

    def loadCities() {

        findOnDisk("villes_france.csv").splitEachLine(",") { fields ->
            String key = String.valueOf(fields[9]).trim()
            map.put(key, [
                    code: key,
                    formattedName: fields[3],
                    name: fields[4],
                    postcode: fields[7],
                    population: fields[13],
                    density: fields[16],
                    surface: fields[17],
                    lat: fields[19],
                    lng: fields[18],
                    altitudeMin: fields[24],
                    altitudeMax: fields[25],
                    populationRank: fields[26],
                    densityRank: fields[27],
                    surfaceRank: fields[28],
                    countryCode: "fr",
            ])

        }


    }

    def start() {

        print "LOADING CITIES"
        loadCities()
        println " -> DONE"



        vertx.fileSystem.readDir(findOnDisk("osm").getAbsolutePath(), ".*\\.shp") { ar ->
            if (ar.succeeded) {

                for (fileName in ar.result) {
                    File folder = new File(fileName)
                    FileDataStore store;
                    try {
                        store = FileDataStoreFinder.getDataStore(findOnDisk("osm/${folder.getName()}/${folder.getName()}"))

                        FeatureSource featureSource = store.getFeatureSource()
                        FeatureCollection featureCollection = featureSource.getFeatures()

                        FeatureIterator featureIterator = featureCollection.features()

                        try {
                            while (featureIterator.hasNext()) {
                                total++
                                SimpleFeature feature = (SimpleFeature) featureIterator.next();


                                String cityCode = ((String) feature.getAttributes().get(2)).trim();

                                if (map.get(cityCode)) {
                                    def city = map.get(cityCode).clone()
                                    map.remove(cityCode)

                                    Geometry geometry = (Geometry) feature.getAttributes().get(0);

                                    def coords = []

                                    geometry.coordinates.eachWithIndex() { p, i ->
                                        def coord = []
                                        coord[0] = p.x
                                        coord[1] = p.y

                                        coords[i] = coord
                                    }

                                    city.put('boundaries', coords)
                                    count++
                                    //println count
                                    sendToElasticSearch(city)
                                }


                            }

                        } finally {
                            featureIterator.close()
                            store.dispose()
                        }

                    } catch (Exception e) {
                        println "Error on => ${folder.getName()} => ${e.message}"
                    }
                }


                println("SUCCESS => ${count}/${total} cities indexed")

            } else {
                println "Failed to read => ${ar.cause}"
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
            retry(response, message)
        }

    }

    private void retry(response, message) {
        if (response.statusCode > 201) {
            vertx.eventBus.send("fr.xebia.dataviz.es.createObject", message) { resp ->
                retry(resp, message)
            }
        }
    }

    private File findOnDisk(String resourceRelativePath) {
        VertxInternal core = vertx.toJavaVertx() as VertxInternal
        String pathToDisk = PathAdjuster.adjust(core, resourceRelativePath)
        new File(pathToDisk)
    }

}
