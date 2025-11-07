# ElasticSearch Upgrade: 2.3.2 → 7.17.18

## Summary

Successfully upgraded ElasticSearch from version 2.3.2 (released 2016) to 7.17.18 (latest 7.x series).

## Changes Made

### Version Upgrade
- **From:** ElasticSearch 2.3.2
- **To:** ElasticSearch 7.17.18
- **Dependency:** Changed from `org.elasticsearch:elasticsearch` to `org.elasticsearch.client:elasticsearch-rest-high-level-client`

### API Migration
Migrated from deprecated TransportClient (TCP-based) to RestHighLevelClient (HTTP-based):

1. **Connection Method**
   - Old: `TransportClient` on port 9300 (TCP)
   - New: `RestHighLevelClient` on port 9200 (HTTP/REST)

2. **API Changes**
   - Old: `client.prepareSearch().setQuery().execute().actionGet()`
   - New: `client.search(new SearchRequest().source(searchSourceBuilder), RequestOptions.DEFAULT)`

### Files Modified

#### webapp (hnjobs/webapp)
- `pom.xml` - Updated ElasticSearch dependency
- `src/main/java/com/emilburzo/hnjobs/server/manager/SearchManager.java` - Migrated to RestHighLevelClient
- `.ci/deploy.yaml` - Updated port from 7938 to 9200
- `docker-compose.yml` - Added for local testing

#### parser (hnjobs/parser)
- `pom.xml` - Updated ElasticSearch dependency
- `src/main/java/com/emilburzo/hnjobs/main/Main.java` - Migrated to RestHighLevelClient
- `src/main/java/com/emilburzo/hnjobs/parser/Parser.java` - Migrated index, search, and delete operations

## Testing Results

All functionality was thoroughly tested with ElasticSearch 7.17.18:

### ✅ Index Operations
- Successfully created `hnjobs` index
- Successfully indexed job documents with all fields (author, timestamp, src, body, bodyHtml)

### ✅ Search Operations
- Simple query string search works correctly
- Sorting by score and timestamp works
- Search highlighting works correctly

### ✅ Phrase Suggester
- Spelling suggestions work (e.g., "Elasticserch" → "elasticsearch")

### ✅ Document Operations
- GET operations retrieve documents correctly
- DELETE operations remove documents successfully

### ✅ Scroll/Cleanup Operations
- Scroll API works for batch processing
- Constant score queries work for cleanup logic

## Deployment Notes

### Port Change
**IMPORTANT:** ElasticSearch port changed from 9300 to 9200

- The old TransportClient used port 9300 (transport protocol)
- The new RestHighLevelClient uses port 9200 (HTTP REST API)
- Updated deployment configuration accordingly

### Environment Variables
The following environment variables continue to work:
- `ELASTICSEARCH_HOST` - ElasticSearch hostname (default: "localhost")
- `ELASTICSEARCH_PORT` - ElasticSearch port (default: "9200", was "9300")

### ElasticSearch Server Upgrade
When upgrading the ElasticSearch server itself:
1. Backup all data
2. Upgrade to ES 7.17.18
3. Ensure port 9200 is accessible (HTTP REST API)
4. Port 9300 (transport) is no longer needed for these applications

## Benefits of Upgrade

1. **Modern API:** Using current REST-based client instead of deprecated TransportClient
2. **Better Performance:** ES 7.x has significant performance improvements
3. **Security Updates:** 9+ years of security patches and bug fixes
4. **Future Proof:** ES 7.x is still maintained, while 2.x is long EOL
5. **Better Features:** Improved search relevance, aggregations, and query capabilities

## Compatibility

- Java 8 compatible
- No breaking changes to search functionality
- All existing queries and features continue to work
- API is forward-compatible with ES 8.x migration path

## Next Steps (Optional)

For future consideration:
- ES 8.x upgrade (requires Java 11+ and new Java API Client)
- Enable security features (xpack.security)
- Add monitoring and metrics
- Configure index lifecycle management (ILM)
