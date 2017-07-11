package org.wso2.carbon.mediator.cache.json;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.synapse.config.xml.MediatorSerializerFinder;

import java.util.List;

/**
 * Created by riyafa on 7/10/17.
 */
public class JSONCacheMediatorSerializer extends AbstractMediatorSerializer {
    @Override
    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof JSONCacheMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        JSONCacheMediator mediator = (JSONCacheMediator) m;
        OMElement cache = fac.createOMElement("jsoncache", synNS);
        saveTracingState(cache, mediator);

        if (mediator.getId() != null) {
            cache.addAttribute(fac.createOMAttribute("id", nullNS, mediator.getId()));
        }

        if (mediator.getScope() != null) {
            cache.addAttribute(fac.createOMAttribute("scope", nullNS, mediator.getScope()));
        }

        if (mediator.isCollector()) {
            cache.addAttribute(fac.createOMAttribute("collector", nullNS, "true"));
        } else {

            cache.addAttribute(fac.createOMAttribute("collector", nullNS, "false"));

            if (mediator.getDigestGenerator() != null) {
                cache.addAttribute(fac.createOMAttribute("hashGenerator", nullNS,
                                                         mediator.getDigestGenerator().getClass().getName()));
            }

            if (mediator.getTimeout() != 0) {
                cache.addAttribute(
                        fac.createOMAttribute("timeout", nullNS, Long.toString(mediator.getTimeout())));
            }

            if (mediator.getMaxMessageSize() != 0) {
                cache.addAttribute(
                        fac.createOMAttribute("maxMessageSize", nullNS,
                                              Integer.toString(mediator.getMaxMessageSize())));
            }

            if (mediator.getOnCacheHitRef() != null) {
                OMElement onCacheHit = fac.createOMElement("onCacheHit", synNS);
                onCacheHit.addAttribute(
                        fac.createOMAttribute("sequence", nullNS, mediator.getOnCacheHitRef()));
                cache.addChild(onCacheHit);
            } else if (mediator.getOnCacheHitSequence() != null) {
                OMElement onCacheHit = fac.createOMElement("onCacheHit", synNS);
                new JSONCacheMediatorSerializer()
                        .serializeChildren(onCacheHit, mediator.getOnCacheHitSequence().getList());
                cache.addChild(onCacheHit);
            }

            if (mediator.getInMemoryCacheSize() != 0) {
                OMElement implElem = fac.createOMElement("implementation", synNS);
                implElem.addAttribute(fac.createOMAttribute("type", nullNS, "memory"));
                implElem.addAttribute(fac.createOMAttribute("maxSize", nullNS,
                                                            Integer.toString(mediator.getInMemoryCacheSize())));
                cache.addChild(implElem);
            }

            if (mediator.getDiskCacheSize() != 0) {
                OMElement implElem = fac.createOMElement("implementation", synNS);
                implElem.addAttribute(fac.createOMAttribute("type", nullNS, "disk"));
                implElem.addAttribute(fac.createOMAttribute("maxSize", nullNS,
                                                            Integer.toString(mediator.getDiskCacheSize())));
                cache.addChild(implElem);
            }
        }

        return cache;
    }

    public String getMediatorClassName() {
        return JSONCacheMediator.class.getName();
    }

    /**
     * Creates XML representation of the child mediators
     *
     * @param parent The mediator for which the XML representation child should be attached
     * @param list   The mediators list for which the XML representation should be created
     */
    protected void serializeChildren(OMElement parent, List<Mediator> list) {
        for (Mediator child : list) {
            MediatorSerializer medSer = MediatorSerializerFinder.getInstance().getSerializer(child);
            if (medSer != null) {
                medSer.serializeMediator(parent, child);
            } else {
                handleException("Unable to find a serializer for mediator : " + child.getType());
            }
        }
    }
}
