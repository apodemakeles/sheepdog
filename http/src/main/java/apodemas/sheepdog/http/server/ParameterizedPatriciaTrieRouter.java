package apodemas.sheepdog.http.server;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.requst.HttpRequestHandler;

import java.util.Arrays;

/**
 * @author caozheng
 * @time 2019-01-19 09:06
 **/
public class ParameterizedPatriciaTrieRouter implements Router {
    private final Node root;

    public ParameterizedPatriciaTrieRouter(){
        root = new Node();
        root.path = "";
    }

    public void add(String path, HttpRequestHandler handler){
        if(path.charAt(0)!='/') {
            path = "/" + path;
        }

        root.insertChild(path, 0, handler);
    }

    public RouteMatchResult match(String path){
        return root.match(path, 0, new PathParams());
    }

    public static class Node{

        private String path;
        private String indices;
        private Node[] childrenNode;
        private int children;
        private HttpRequestHandler handler;
        private boolean paramChild;
        private boolean isParam;

        public Node(){
            this.path = "";
            this.indices = "";
            this.childrenNode = new Node[0];
        }


        public void insertChild(String cs, int start, HttpRequestHandler h) {
            if (start >= cs.length()) {
                if (this.handler != null) {
                    throw new IllegalRoutePathException("Handlers are already registered", cs);
                }

                this.handler = h;

                return;
            }

            char c = cs.charAt(start);

            if (c == ':') {
                int i = start + 1;
                boolean hasSlash = false;
                for (; i < cs.length(); i++) {
                    if (cs.charAt(i) == '/') {
                        hasSlash = true;
                        break;
                    } else if (cs.charAt(i) == ':') {
                        throw new IllegalRoutePathException("Each segment can contain only one parameter", cs);
                    }
                }
                if ((hasSlash && (i - start == 1)) || (!hasSlash && start == i)) {
                    throw new IllegalRoutePathException("Parameter must be named with a non-empty name", cs);
                }
                String path = cs.substring(start, i);
                if (this.children == 0) {
                    Node node = setParamNode(path);
                    node.insertChild(cs, hasSlash ? i + 1 : i, h);
                } else if (!this.paramChild) {
                    throw new IllegalRoutePathException("Conflict with existing static path", cs);
                } else {
                    Node node = this.childrenNode[0];
                    if (!node.path.equals(path)) {
                        throw new IllegalRoutePathException("Conflict with existing parameter path", cs);
                    }
                    node.insertChild(cs, hasSlash ? i + 1 : i, h);
                }
            }
            else {
                int i = start;
                for (; i < cs.length() && cs.charAt(i) != ':'; i++) {
                }
                String path = cs.substring(start, i);
                int pos = indices.indexOf(path.charAt(0));
                if (pos == -1) {
                    Node node = addStaticNode(path);
                    node.insertChild(cs, i, h);
                } else {
                    Node node = childrenNode[pos];
                    int len = Math.min(path.length(), node.path.length());
                    int j = 1;
                    for (; j < len; j++) {
                        if (path.charAt(j) != node.path.charAt(j)) {
                            break;
                        }
                    }
                    if (j != node.path.length()) {
                        splitNode(node, j);

                    }

                    node.insertChild(cs, start + j, h);
                }
            }
        }

        public RouteMatchResult match(String fullPath, int start, PathParams params) {
            if (StringUtils.empty(path)) {
                return findInChildren(fullPath, start, params);
            }

            if(isParam) {
                int slash = fullPath.indexOf('/', start);
                boolean end = false;
                if (slash == -1) {
                    slash = fullPath.length();
                    end = true;
                } else if (slash == fullPath.length() - 1) {
                    end = true;
                }

                params.add(path.substring(1, path.length()), fullPath.substring(start, slash));

                if (end) {
                    return handler != null ? new RouteMatchResult(handler, params) : RouteMatchResult.NOT_MATCH;
                } else {
                    return findInChildren(fullPath, slash + 1, params);
                }
            }

            int len = fullPath.length() - start;
            if (len >= path.length()) {
                String cur = fullPath.substring(start, start + path.length());
                if (cur.equals(path)) {
                    if (len == path.length()) {
                        return handler != null ? new RouteMatchResult(handler, params) : RouteMatchResult.NOT_MATCH;
                    } else {
                        return findInChildren(fullPath, start + path.length(), params);
                    }
                }
            }

            return RouteMatchResult.NOT_MATCH;
        }

        private RouteMatchResult findInChildren(String fullPath, int start, PathParams params){
            if(paramChild){
                return childrenNode[0].match(fullPath, start, params);
            }else {
                char index = fullPath.charAt(start);
                int pos = indices.indexOf(index);
                if (pos >= 0) {
                    return childrenNode[pos].match(fullPath, start, params);
                }
            }

            return RouteMatchResult.NOT_MATCH;
        }

        private Node setParamNode(String path){
            Node node = new Node();
            node.path = path;
            node.isParam = true;

            this.paramChild = true;
            this.children = 1;
            this.childrenNode = new Node[]{ node};

            return node;
        }

        private Node addStaticNode(String path) {
            Node node = new Node();
            node.path = path;
            node.isParam = false;
            if (children == 0) {
                childrenNode = new Node[4];
            } else if (children == childrenNode.length) {
                childrenNode = Arrays.copyOf(childrenNode, children * 2);
            }

            childrenNode[children++] = node;
            indices += path.charAt(0);

            return node;
        }

        private void splitNode(Node node, int endIndex){
            Node newNode = new Node();
            newNode.children = node.children;
            newNode.childrenNode = node.childrenNode;
            newNode.indices = node.indices;
            newNode.isParam = node.isParam;
            newNode.paramChild = node.paramChild;
            newNode.handler = node.handler;
            newNode.path = node.path.substring(endIndex);

            node.children = 1;
            node.childrenNode = new Node[4];
            node.childrenNode[0] = newNode;
            node.path = node.path.substring(0, endIndex);
            node.indices = ""  + newNode.path.charAt(0);
            node.isParam = false;
            node.paramChild = false;
            node.handler = null;
        }

    }
}
