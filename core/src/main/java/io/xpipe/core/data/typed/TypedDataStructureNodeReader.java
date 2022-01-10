package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.*;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.DataTypeVisitors;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class TypedDataStructureNodeReader implements TypedAbstractReader {

    public static TypedDataStructureNodeReader mutable(DataType type) {
        return new TypedDataStructureNodeReader(type, false);
    }

    public static TypedDataStructureNodeReader immutable(DataType type) {
        return new TypedDataStructureNodeReader(type, true);
    }

    private final boolean makeImmutable;
    private DataStructureNode readNode;

    private final Stack<List<DataStructureNode>> children;
    private final Stack<DataStructureNode> nodes;
    private int arrayDepth;

    private final List<DataType> flattened;
    private DataType expectedType;
    private int currentExpectedTypeIndex;

    private TypedDataStructureNodeReader(DataType type, boolean makeImmutable) {
        flattened = new ArrayList<>();
        type.visit(DataTypeVisitors.flatten(flattened::add));
        children = new Stack<>();
        nodes = new Stack<>();
        this.makeImmutable = makeImmutable;
        expectedType = flattened.get(0);
    }

    @Override
    public void onNodeBegin() {
        if (nodes.size() != 0 || children.size() != 0) {
            throw new IllegalStateException("Reader did not completely reset");
        }

        readNode = null;
    }

    @Override
    public boolean isDone() {
        return readNode != null;
    }

    public DataStructureNode create() {
        if (readNode == null) {
            throw new IllegalStateException("Reader is not finished yet");
        }

        return readNode;
    }

    @Override
    public void onNodeEnd() {
        if (nodes.size() != 0 || children.size() != 0 || readNode == null) {
            throw new IllegalStateException("Reader is not finished yet");
        }
    }

    private void finishNode(DataStructureNode node) {
        if (nodes.empty()) {
            readNode = node;
        } else {
            children.peek().add(node);
        }
    }

    @Override
    public void onValue(byte[] data) {
        if (!expectedType.isValue()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got value");
        }

        var val = makeImmutable ? ValueNode.immutable(data) : ValueNode.mutable(data);
        finishNode(val);
        moveExpectedType(false);
    }

    private boolean isInArray() {
        return arrayDepth >= 1;
    }

    @Override
    public void onGenericNode(DataStructureNode node) {
        if (!expectedType.isWildcard()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got generic node");
        }

        finishNode(makeImmutable ? node.immutableView() : node);
        moveExpectedType(false);
    }

    @Override
    public void onTupleBegin(TupleType type) {
        if (!expectedType.isTuple()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got tuple");
        }

        TupleType tupleType = expectedType.asTuple();
        moveExpectedType(false);

        var l = new ArrayList<DataStructureNode>(tupleType.getSize());
        children.push(l);

        var tupleNames = makeImmutable ?
                Collections.unmodifiableList(tupleType.getNames()) : new ArrayList<>(tupleType.getNames());
        var tupleNodes = makeImmutable ? Collections.unmodifiableList(l) : l;
        var newNode = TupleNode.of(!makeImmutable, tupleNames, tupleNodes);
        nodes.push(newNode);
    }

    @Override
    public void onTupleEnd() {
        children.pop();
        var popped = nodes.pop();
        if (!popped.isTuple()) {
            throw new IllegalStateException("No tuple to end");
        }

        TupleNode tuple = popped.asTuple();
        if (tuple.getKeyNames().size() != tuple.getNodes().size()) {
            throw new IllegalStateException("Tuple node size mismatch");
        }

        finishNode(popped);
    }

    private void moveExpectedType(boolean force) {
        if (!isInArray() || force) {
            currentExpectedTypeIndex++;
            expectedType = currentExpectedTypeIndex == flattened.size() ? null : flattened.get(currentExpectedTypeIndex);
        }
    }

    @Override
    public void onArrayBegin(int size) {
        if (!expectedType.isArray()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got array");
        }

        arrayDepth++;
        moveExpectedType(true);

        var l = new ArrayList<DataStructureNode>();
        children.push(l);

        var arrayNodes = makeImmutable ? Collections.unmodifiableList(l) : l;
        var newNode = ArrayNode.of(arrayNodes);
        nodes.push(newNode);
    }

    @Override
    public void onArrayEnd() {
        if (!isInArray()) {
            throw new IllegalStateException("No array to end");
        }

        arrayDepth--;
        moveExpectedType(true);

        children.pop();
        var popped = nodes.pop();
        finishNode(popped);
    }
}