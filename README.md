geographic-data
===============

This project is intended to buil the "cities" index on ElasticSearch.

## Resources

Geographic data comes from OpenStreetMap and is stored in [osm resource directory][osm].

Demographic data comes from [https://github.com/Matael/budgetcommunes]() or [here](https://ent.univ-paris13.fr/applications/claroline/claroline/document/document.php?cmd=exChDir&file=%2FVFAz&sort=date&dir=3&cidReset=true&cidReq=UEINFO) and are stored in [villes_frances.csv][].
Some cities are missing, so if the "cities" index became more important, you should re-build it from fresher sources on [French gov. open data website](http://data.gouv.fr).

Cities from [villes_frances.csv][] are went through. For each city, its shape is looked for in [Open Street Map data][osm] (that's why missing cities in [villes_frances.csv][] are an issue).

## Install

### Missing dependencies

In order to build successfully this project, you must install in you `.m2` directory an artifact that cannot be published on maven central due to licencing problem.

From the project root directory:

	PROJ_DIR=$PWD
	mkdir -p ~/.m2/repository/javax
	cd ~/.m2/repository/javax
	tar jxvf "${PROJ_DIR}/libs/jai_core.tar.bz2"

### Configure

Edit [conf.json][conf], it looks like:

	{
		"host": "es-a",
		"cluster":"devVPC",
		"port": 9300,
		"startIndex":1
	}

- Change `host` value to match one you ElasticSearch cluster instance (IP or FQDN)
- Change `cluster` value to match the ElasticSearch cluster name (cf ElasticSearch config of your instances)

### Build

From the projet root directory:

	./gradlew clean && ./gradlew assemble

## Run

From the projet root directory:

	nohup ./gradlew runMod 1>../geodata.log 2>../geodata-err.log

You can follow the import process with `tail -F ../geodata.log ../geodata-err.log`

[villes_frances.csv]: ./main/resources/villes_france.csv
[osm]: ./main/resources/osm
[conf]: ./conf.json