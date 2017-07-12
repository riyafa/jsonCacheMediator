package org.wso2.carbon.mediator.cache.json.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.SequenceMediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.wso2.carbon.mediator.cache.json.CachingConstants;
import org.wso2.carbon.mediator.cache.json.digest.DigestGenerator;
import org.wso2.carbon.mediator.cache.json.JSONCacheMediator;

import java.util.Iterator;
import java.util.Properties;
import javax.xml.namespace.QName;

public class JSONCacheMediatorFactory extends AbstractMediatorFactory {
    /**
     * Log object to use when logging is required in this class.
     */
    private static final Log log = LogFactory.getLog(JSONCacheMediatorFactory.class);

    /**
     * QName of the ID of cache configuration
     */
    private static final QName ATT_ID = new QName("id");

    /**
     * QName of the collector
     */
    private static final QName ATT_COLLECTOR = new QName("collector");

    /**
     * QName of the digest generator
     */
    private static final QName ATT_HASH_GENERATOR = new QName("hashGenerator");

    /**
     * QName of the maximum message size
     */
    private static final QName ATT_MAX_MSG_SIZE = new QName("maxMessageSize");

    /**
     * QName of the timeout
     */
    private static final QName ATT_TIMEOUT = new QName("timeout");

    /**
     * QName of the cache scope
     */
    private static final QName ATT_SCOPE = new QName("scope");

    /**
     * QName of the mediator sequence
     */
    private static final QName ATT_SEQUENCE = new QName("sequence");

    /**
     * QName of the implementation type
     */
    private static final QName ATT_TYPE = new QName("type");

    /**
     * QName of the maximum message size
     */
    private static final QName ATT_SIZE = new QName("maxSize");

    /**
     * QName of the onCacheHit mediator sequence reference
     */
    private static final QName ON_CACHE_HIT_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "onCacheHit");

    /**
     * QName of the cache implementation
     */
    private static final QName IMPLEMENTATION_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "implementation");

    /**
     * This holds the default timeout of the mediator cache
     */
    private static final long DEFAULT_TIMEOUT = 5000L;

    /**
     * This holds the default disk cache size used in cache mediator
     */
    private static final int DEFAULT_DISK_CACHE_SIZE = 200;

    public QName getTagQName() {
        return CachingConstants.CACHE_Q;
    }

    @Override
    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        if (!CachingConstants.CACHE_Q.equals(elem.getQName())) {
            handleException(
                    "Unable to create the cache mediator. Unexpected element as the cache mediator configuration");
        }

        JSONCacheMediator cache = new JSONCacheMediator();
        OMAttribute idAttr = elem.getAttribute(ATT_ID);
        if (idAttr != null && idAttr.getAttributeValue() != null) {
            cache.setId(idAttr.getAttributeValue());
        }

        OMAttribute scopeAttr = elem.getAttribute(ATT_SCOPE);
        if (scopeAttr != null && scopeAttr.getAttributeValue() != null &&
                isValidScope(scopeAttr.getAttributeValue(), cache.getId())) {
            cache.setScope(scopeAttr.getAttributeValue());
        } else {
            cache.setScope(CachingConstants.SCOPE_PER_HOST);
        }

        OMAttribute collectorAttr = elem.getAttribute(ATT_COLLECTOR);
        if (collectorAttr != null && collectorAttr.getAttributeValue() != null &&
                "true".equals(collectorAttr.getAttributeValue())) {

            cache.setCollector(true);
        } else {

            cache.setCollector(false);

            OMAttribute hashGeneratorAttr = elem.getAttribute(ATT_HASH_GENERATOR);
            if (hashGeneratorAttr != null && hashGeneratorAttr.getAttributeValue() != null) {
                try {
                    Class generator = Class.forName(hashGeneratorAttr.getAttributeValue());
                    Object o = generator.newInstance();
                    if (o instanceof DigestGenerator) {
                        cache.setDigestGenerator((DigestGenerator) o);
                    } else {
                        handleException("Specified class for the hashGenerator is not a " +
                                                "DigestGenerator. It *must* implement " +
                                                "org.wso2.carbon.mediator.cache.digest.DigestGenerator interface");
                    }
                } catch (ClassNotFoundException e) {
                    handleException("Unable to load the hash generator class", e);
                } catch (IllegalAccessException e) {
                    handleException("Unable to access the hash generator class", e);
                } catch (InstantiationException e) {
                    handleException("Unable to instantiate the hash generator class", e);
                }
            }

            OMAttribute timeoutAttr = elem.getAttribute(ATT_TIMEOUT);
            if (timeoutAttr != null && timeoutAttr.getAttributeValue() != null) {
                cache.setTimeout(Long.parseLong(timeoutAttr.getAttributeValue()));
            } else {
                cache.setTimeout(DEFAULT_TIMEOUT);
            }

            OMAttribute maxMessageSizeAttr = elem.getAttribute(ATT_MAX_MSG_SIZE);
            if (maxMessageSizeAttr != null && maxMessageSizeAttr.getAttributeValue() != null) {
                cache.setMaxMessageSize(Integer.parseInt(maxMessageSizeAttr.getAttributeValue()));
            }

            OMElement onCacheHitElem = elem.getFirstChildWithName(ON_CACHE_HIT_Q);
            if (onCacheHitElem != null) {
                OMAttribute sequenceAttr = onCacheHitElem.getAttribute(ATT_SEQUENCE);
                if (sequenceAttr != null && sequenceAttr.getAttributeValue() != null) {
                    cache.setOnCacheHitRef(sequenceAttr.getAttributeValue());
                } else if (onCacheHitElem.getFirstElement() != null) {
                    cache.setOnCacheHitSequence(new SequenceMediatorFactory()
                                                        .createAnonymousSequence(onCacheHitElem, properties));
                }
            }

            for (Iterator<OMElement> itr = elem.getChildrenWithName(IMPLEMENTATION_Q); itr.hasNext(); ) {
                OMElement implElem = itr.next();
                OMAttribute typeAttr = implElem.getAttribute(ATT_TYPE);
                OMAttribute sizeAttr = implElem.getAttribute(ATT_SIZE);
                if (typeAttr != null && typeAttr.getAttributeValue() != null) {
                    String type = typeAttr.getAttributeValue();
                    if (CachingConstants.TYPE_MEMORY.equals(type) && sizeAttr != null &&
                            sizeAttr.getAttributeValue() != null) {
                        cache.setInMemoryCacheSize(Integer.parseInt(sizeAttr.getAttributeValue()));
                    } else if (CachingConstants.TYPE_DISK.equals(type)) {
                        log.warn("Disk based and hierarchical caching is not implemented yet");
                        if (sizeAttr != null && sizeAttr.getAttributeValue() != null) {
                            cache.setDiskCacheSize(Integer.parseInt(sizeAttr.getAttributeValue()));
                        } else {
                            cache.setDiskCacheSize(DEFAULT_DISK_CACHE_SIZE);
                        }
                    } else {
                        handleException("unknown implementation type for the Cache mediator");
                    }
                }
            }
        }

        return cache;
    }

    /**
     * Checks the validity of the provided cache scope in cache mediator configuration
     *
     * @param scope value of the scope attribute parsed in configuration
     * @param id    value of the id attribute parsed in configuration
     * @return boolean value whether the scope is valid or not
     */
    private boolean isValidScope(String scope, String id) {
        if (CachingConstants.SCOPE_PER_HOST.equals(scope)) {
            return true;
        } else if (CachingConstants.SCOPE_PER_MEDIATOR.equals(scope)) {
            if (id != null) {
                return true;
            } else {
                handleException("Id is required for a cache with scope : " + scope);
                return false;
            }
        } else if (CachingConstants.SCOPE_DISTRIBUTED.equals(scope)) {
            return true;
        } else {
            handleException("Unknown scope " + scope + " for the Cache mediator");
            return false;
        }
    }


}
