package org.exist.xquery.functions.text;

import org.exist.xquery.*;
import org.exist.xquery.util.Error;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Item;
import org.exist.dom.*;
import org.exist.indexing.impl.NGramIndex;
import org.exist.indexing.impl.NGramIndexWorker;

import java.util.List;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: wolf
 * Date: 28-Feb-2007
 * Time: 15:18:59
 * To change this template use File | Settings | File Templates.
 */
public class NGramSearch extends Function {

    public final static FunctionSignature signature =
            new FunctionSignature(
                    new QName("ngram-contains", TextModule.NAMESPACE_URI, TextModule.PREFIX),
                    "",
                    new SequenceType[] {
                            new SequenceType(Type.NODE, Cardinality.ZERO_OR_MORE),
                            new SequenceType(Type.STRING, Cardinality.ZERO_OR_ONE)
                    },
                    new SequenceType(Type.NODE, Cardinality.ZERO_OR_MORE)
            );

    public NGramSearch(XQueryContext context) {
        super(context, signature);
    }


    public void setArguments(List arguments) throws XPathException {
        Expression path = (Expression) arguments.get(0);
        steps.add(path);

        Expression arg = (Expression) arguments.get(1);
        arg = new DynamicCardinalityCheck(context, Cardinality.ZERO_OR_ONE, arg,
                new org.exist.xquery.util.Error(Error.FUNC_PARAM_CARDINALITY, "2", mySignature));
        if(!Type.subTypeOf(arg.returnsType(), Type.ATOMIC))
            arg = new Atomize(context, arg);
        steps.add(arg);
    }


    public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
        if (contextItem != null)
			contextSequence = contextItem.toSequence();
        Sequence input = getArgument(0).eval(contextSequence, contextItem);
        NodeSet result = null;
        if (input.isEmpty())
            result = NodeSet.EMPTY_SET;
        else {
            NodeSet inNodes = input.toNodeSet();
            DocumentSet docs = inNodes.getDocumentSet();
            NGramIndexWorker index = (NGramIndexWorker)
                    context.getBroker().getIndexController().getIndexWorkerById(NGramIndex.ID);
            String key = getArgument(1).eval(contextSequence, contextItem).getStringValue();
            String[] ngrams = index.getDistinctNGrams(key);
            for (int i = 0; i < ngrams.length; i++) {
                NodeSet nodes = index.search(docs, null, ngrams[i], context, inNodes, NodeSet.ANCESTOR);
                if (result == null)
                    result = nodes;
                else {
                    NodeSet temp = new ExtArrayNodeSet();
                    for (NodeSetIterator iterator = nodes.iterator(); iterator.hasNext();) {
                        NodeProxy next = (NodeProxy) iterator.next();
                        NodeProxy before = result.get(next);
                        if (before != null) {
                            boolean found = false;
                            Match mb = before.getMatches();
                            while (mb != null && !found) {
                                Match mn = next.getMatches();
                                while (mn != null && !found) {
                                    if (mb.isNear(mn, index.getN()))
                                        found = true;
                                    mn = mn.getNextMatch();
                                }
                                mb = mb.getNextMatch();
                            }
                            if (found)
                                temp.add(next);
                        }
                    }
                    result = temp;
                    LOG.debug("Found " + temp.getLength() + " for: " + ngrams[i]);
                }
            }
        }
        return result;
    }

    public int getDependencies() {
        final Expression stringArg = getArgument(0);
        if (Type.subTypeOf(stringArg.returnsType(), Type.NODE) &&
            !Dependency.dependsOn(stringArg, Dependency.CONTEXT_ITEM)) {
            return Dependency.CONTEXT_SET;
        } else {
            return Dependency.CONTEXT_SET + Dependency.CONTEXT_ITEM;
        }
    }

    public int returnsType() {
        return Type.NODE;
    }
}