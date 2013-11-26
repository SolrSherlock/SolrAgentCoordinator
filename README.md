# SolrAgentCoordinator #
Status: *alpha*<br/>
Latest edit: 20131126
## Background ##
SolrAgentCoordinator serves as a **blackboard** which holds documents submitted by SolrInterceptor, to be accessed by agents running in the SolrAgentFramework.

The platform exists as a stand-alone entity, receiving documents from the SolrCloud (many Solr servers), and communicating over TCP sockets with possibly many instances of SolrAgentFramework.

As a blackboard system, agents can *read* Solr documents to process; they can return documents of their own to the coordinator, setting up *work flows* of processes among themselves.

## Changes ##
20131126 Upgraded to Solr 4.5.1, upgraded to new core API libraries

Upgraded to Solr 4.3.1
Moved code to work with simple json library

## ToDo ##
Mavenize the project<br/>
Create a full unit test suite

## License ##
Apache 2
