package com.citytechinc.aem.prosper.specs

import groovy.transform.Synchronized
import org.apache.sling.commons.testing.jcr.RepositoryUtil
import org.apache.sling.jcr.api.SlingRepository
import spock.lang.Shared
import spock.lang.Specification

import javax.jcr.Node
import javax.jcr.Session

/**
 * Basic AEM spec that includes a transient JCR session.
 */
@SuppressWarnings("deprecated")
abstract class AemSpec extends Specification {

    private static final def SYSTEM_NODE_NAMES = ["jcr:system", "rep:policy"]

    private static final def NODE_TYPES = ["sling", "replication", "tagging", "core", "dam", "vlt", "widgets"]

    private static SlingRepository repository

    @Shared
    private Session sessionInternal

    // global fixtures

    /**
     * Create an administrative JCR session.
     */
    def setupSpec() {
        sessionInternal = getRepository().loginAdministrative(null)
        registerCustomNodeTypes()
    }

    /**
     * Remove all non-system nodes to cleanup any test data and logout of the JCR session.
     */
    def cleanupSpec() {
        removeAllNodes()

        session.logout()
    }

    /**
     * @return admin session
     */
    Session getSession() {
        sessionInternal
    }

    /**
     * Get the Node for a path.
     *
     * @param path valid JCR Node path
     * @return node for given path
     */
    Node getNode(String path) {
        session.getNode(path)
    }

    /**
     * Remove all non-system nodes to cleanup any test data.  This method would typically be called from a test fixture
     * method to cleanup content before the entire specification has been executed.
     */
    void removeAllNodes() {
        session.rootNode.nodes.findAll { !SYSTEM_NODE_NAMES.contains(it.name) }*.remove()
        session.save()
    }

    /**
     * Add JCR namespaces and node types based on any number of CND file input streams.  Specs should override this
     * method to add CND files to be registered at runtime.
     *
     * @return list of InputStreams to add
     */
    List<InputStream> addCndInputStreams() {
        Collections.emptyList()
    }

    // internals

    @Synchronized
    protected def getRepository() {
        if (!repository) {
            RepositoryUtil.startRepository()

            repository = RepositoryUtil.getRepository()

            registerCoreNodeTypes()

            addShutdownHook {
                RepositoryUtil.stopRepository()
            }
        }

        repository
    }

    protected def registerCoreNodeTypes() {
        def session = repository.loginAdministrative(null)

        NODE_TYPES.each { type ->
            this.class.getResourceAsStream("/SLING-INF/nodetypes/${type}.cnd").withStream { stream ->
                RepositoryUtil.registerNodeType(session, stream)
            }
        }

        session.logout()
    }

    protected def registerCustomNodeTypes() {
        def session = repository.loginAdministrative(null)

        addCndInputStreams().each { RepositoryUtil.registerNodeType(session, it) }

        session.logout()
    }

}
