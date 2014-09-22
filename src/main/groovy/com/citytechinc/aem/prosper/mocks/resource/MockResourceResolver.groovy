package com.citytechinc.aem.prosper.mocks.resource

import org.apache.sling.api.adapter.AdapterFactory
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.jcr.resource.JcrResourceUtil
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProvider

import javax.jcr.Node
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.servlet.http.HttpServletRequest

@SuppressWarnings("deprecation")
class MockResourceResolver implements TestResourceResolver, GroovyInterceptable {

    private final JcrResourceProvider resourceProvider

    private final Session session

    private final Map<Class, Closure> resourceResolverAdapters

    private final Map<Class, Closure> resourceAdapters

    private final List<AdapterFactory> adapterFactories

    private String[] searchPath

    private boolean closed

    MockResourceResolver(Session session, Map<Class, Closure> resourceResolverAdapters,
        Map<Class, Closure> resourceAdapters, List<AdapterFactory> adapterFactories) {
        this.resourceProvider = new JcrResourceProvider(session, null, false)
        this.session = session
        this.resourceResolverAdapters = resourceResolverAdapters
        this.resourceAdapters = resourceAdapters
        this.adapterFactories = adapterFactories
    }

    @Override
    void addResourceAdapter(Class adapterType, Closure closure) {
        resourceAdapters.put(adapterType, closure)
    }

    @Override
    void addResourceResolverAdapter(Class adapterType, Closure closure) {
        resourceResolverAdapters.put(adapterType, closure)
    }

    @Override
    void setSearchPath(String... searchPath) {
        this.searchPath = searchPath
    }

    @Override
    def invokeMethod(String name, args) {
        if (["isLive", "close"].contains(name) || !closed) {
            return this.&"$name"(args)
        }

        throw new IllegalStateException("The resource resolver is closed.")
    }

    @Override
    Resource getResource(String path) {
        def resource = null

        try {
            if (session.itemExists(path)) {
                resource = getResourceInternal(path)
            }
        } catch (RepositoryException e) {
            // ignore
        }

        resource
    }

    @Override
    Resource getResource(Resource base, String path) {
        def resource

        if (path.startsWith("/")) {
            resource = getResource(path)
        } else {
            resource = base ? getResource("${base.path}/$path") : null
        }

        resource
    }

    @Override
    ResourceResolver clone(Map<String, Object> authenticationInfo) {
        throw new UnsupportedOperationException()
    }

    @Override
    Iterator<Resource> findResources(String query, String language) {
        def resourceResults = JcrResourceUtil.query(session, query, language).nodes.collect() { Node node ->
            getResource(node.path)
        }

        resourceResults.iterator()
    }

    @Override
    String[] getSearchPath() {
        searchPath ?: [] as String[]
    }

    @Override
    Iterator<Resource> listChildren(Resource parent) {
        getChildren(parent).iterator()
    }

    @Override
    Iterable<Resource> getChildren(Resource parent) {
        parent.adaptTo(Node).nodes.collect { Node node ->
            getResourceInternal(node.path)
        } as Iterable<Resource>
    }

    @Override
    String map(String resourcePath) {
        resourcePath
    }

    @Override
    String map(HttpServletRequest request, String resourcePath) {
        resourcePath
    }

    @Override
    Iterator<Map<String, Object>> queryResources(String query, String language) {
        throw new UnsupportedOperationException()
    }

    @Override
    Resource resolve(HttpServletRequest request, String absPath) {
        resolve(absPath)
    }

    @Override
    Resource resolve(HttpServletRequest request) {
        throw new UnsupportedOperationException()
    }

    @Override
    Resource resolve(String absPath) {
        getResource(absPath) ?: new MockNonExistingResource(this, absPath, resourceAdapters, adapterFactories)
    }

    @Override
    <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        def result = (AdapterType) adapterFactories.findResult {
            adapterFactory -> adapterFactory.getAdapter(this, type)
        }

        if (!result) {
            def adapter = resourceResolverAdapters.find { it.key == type }

            if (adapter) {
                result = (AdapterType) adapter.value.call(this)
            }
        }

        result
    }

    @Override
    boolean isLive() {
        !closed
    }

    @Override
    void close() {
        closed = true
    }

    @Override
    String getUserID() {
        throw new UnsupportedOperationException()
    }

    @Override
    Object getAttribute(String name) {
        resourceProvider.getAttribute(this, name)
    }

    @Override
    Iterator<String> getAttributeNames() {
        resourceProvider.getAttributeNames(this)
    }

    @Override
    void delete(Resource resource) {
        throw new UnsupportedOperationException()
    }

    @Override
    Resource create(Resource parent, String name, Map<String, Object> properties) {
        throw new UnsupportedOperationException()
    }

    @Override
    void revert() {
        resourceProvider.revert(this)
    }

    @Override
    void commit() {
        resourceProvider.commit(this)
    }

    @Override
    boolean hasChanges() {
        resourceProvider.hasChanges(this)
    }

    private Resource getResourceInternal(String path) {
        def jcrResource = resourceProvider.getResource(this, path)

        new MockResource(jcrResource, resourceAdapters, adapterFactories)
    }
}
