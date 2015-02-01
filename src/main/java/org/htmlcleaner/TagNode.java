/*  Copyright (c) 2006-2014, HTMLCleaner project
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of HtmlCleaner may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
    
    http://htmlcleaner.sourceforge.net/
*/

package org.htmlcleaner;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import org.htmlcleaner.conditional.ITagNodeCondition;
import org.htmlcleaner.conditional.TagAllCondition;
import org.htmlcleaner.conditional.TagNodeAttExistsCondition;
import org.htmlcleaner.conditional.TagNodeAttValueCondition;
import org.htmlcleaner.conditional.TagNodeNameCondition;

/**
 * <p>
 *      XML node tag - basic node of the cleaned HTML tree. At the same time, it represents start tag token
 *      after HTML parsing phase and before cleaning phase. After cleaning process, tree structure remains
 *      containing tag nodes (TagNode class), content (text nodes - ContentNode), comments (CommentNode)
 *      and optionally doctype node (DoctypeToken).
 * </p>
 */
public class TagNode extends TagToken implements HtmlNode {
    private TagNode parent;
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();
    private final List<BaseToken> children = new ArrayList<BaseToken>();
    private DoctypeToken docType;
    private List<BaseToken> itemsToMove;
    private Map<String, String> nsDeclarations;

    private transient boolean isFormed;

    /**
     * Used to indicate a start tag that was auto generated because {@link TagInfo#isContinueAfter(String)}(closedTag.getName()) returned true
     * For example,
     * <pre>
     * <b><i>foo</b>bar
     * </pre>
     * would result in a new <i> being created resulting in
     * <pre>
     * <b><i>foo</i></b><i>bar</i>
     * </pre>
     * The second opening <i> tag is marked as autogenerated. This allows the autogenerated tag to be removed if it is unneeded.
     */
    private boolean autoGenerated;
    
    /**
     * This flag is set if we are using namespace aware setting, and the tagnode belongs
     * to a non-HTML namespace. 
     */
    private boolean isForeignMarkup;
    
    /**
     * This flag is set if foreignMarkup is set; if it is false it means that the tagnode tree has not been built and so
     * it isn't known whether this node is a HTML node or foreign markup such as SVG.
     */
    private boolean foreignMarkupFlagSet = false;

    /**
     * Indicates that the node was marked to be pruned out of the tree.
     */
    private boolean pruned;

    /**
     * Indicates that the node is a copy of another node.
     * @see #makeCopy()
     */
    private final boolean isCopy;

    public TagNode(String name) {
        this(name, false);
    }

    private TagNode(String name, boolean isCopy) {
        super(name);
        this.isCopy = isCopy;
    }
    
	/* (non-Javadoc)
	 * @see org.htmlcleaner.TagToken#getName()
	 */
	@Override
	public String getName() {
		//
		// If this is foreign markup (e.g. SVG) we return the
		// original name, otherwise we return it in lower case
		//
		if (this.isForeignMarkup){
			return name;
		} else {
			return name == null ? null: name.toLowerCase();
		}
	}
    

    /**
     * @param attName
     * @return Value of the specified attribute, or null if it this tag doesn't contain it.
     */
    public String getAttributeByName(String attName) {    	
    	if (attName == null) return null;
    	//
    	// We have to do case-insensitive comparisons
    	//	
        return attName != null ? (String) attributes.get(attName.toLowerCase()) : null;
    }

    /**
     * Returns the attributes of the tagnode. 
     * 
     * @return Map instance containing all attribute name/value pairs.
     */
    public Map<String, String> getAttributes() {
    	return new LinkedHashMap<String, String>(this.attributes);
    }
    
    /**
     * Returns the attributes of the tagnode in lower case. 
     * 
     * @return Map instance containing all attribute name/value pairs, with attribute names transformed to lower case
     */
    public Map<String, String> getAttributesInLowerCase(){
    	return attributesToLowerCase();
    }

    /**
     * Replace the current set of attributes with a new set. 
     * @param attributes
     */
    public void setAttributes(Map<String, String> attributes) {

    	//
    	// If we haven't yet built the tree, we don't know if this
    	// element is "foreign markup". In this case we don't want
    	// to overwrite attributes with the same version with a lower
    	// cased name when its set by the transforms processor.
    	//

    	//
    	// We're calling this method after the tree has been built,
    	// so its safe to just set the attributes
    	//
    	if (foreignMarkupFlagSet){
    		replaceAttributes(attributes);
    	} else {
    		//
    		// The foreign markup flag hasn't been set, so instead of just
    		// replacing the contents of the attributes map, we iterate
    		// over it and use the original case name from the existing
    		// attributes map where it exists
    		//

    		//
    		// First create a map to hold the processed map contents
    		//
    		LinkedHashMap<String, String> processedAttributes = new LinkedHashMap<String, String>();

    		//
    		// Iterate over the keys in the map provided by the transforms processor
    		// and add them to the set of processed keys
    		//
    		for (String key: attributes.keySet()){

    			String keyToSet = key; // the key to set
    			String value = attributes.get(key); // the value to set

    			//
    			// Check to see if the key exists in the current attribute set
    			// with different casing. If so, we keep the casing
    			//
    			if (!foreignMarkupFlagSet){
    				for (String existingKey: this.attributes.keySet()){
    					if (existingKey.equalsIgnoreCase(key)){
    						keyToSet = existingKey;
    					}
    				}
    			}
    			processedAttributes.put(keyToSet, value);    		
    		}
    		replaceAttributes(processedAttributes);
    	}
    }
    
    /**
     *    
     * Clears existing attributes and puts replacement attributes
     * @param attributes the attributes to set
     */
    private void replaceAttributes(Map<String, String> attributes){

    	this.attributes.clear();
    	this.attributes.putAll(attributes);    	
    }

    /**
     * Checks existence of specified attribute.
     *
     * @param attName
     * @return true if TagNode has attribute
     */
    public boolean hasAttribute(String attName) {
    	if (attName == null) return false;
    	
    	//
    	// We have to do case-insensitive comparisons
    	//
    	for (String key: attributes.keySet()){
    		if (key.equalsIgnoreCase(attName)) return true;
    	}
    	
    	return false;
    }

    /**
     * Adds specified attribute to this tag or overrides existing one.
     * 
     * @param attName
     * @param attValue
     */
    @Override
    public void addAttribute(String attName, String attValue) {
        if (attName != null) {
            String trim = attName.trim();
            if (!isForeignMarkup && foreignMarkupFlagSet) trim = trim.toLowerCase();
            String value = attValue == null ? "" : attValue.trim().replaceAll("\\p{Cntrl}", " ");
            if (trim.length() != 0) {
                attributes.put(trim, value);
            }
        }
    }

    /**
     * Removes specified attribute from this tag.
     *
     * @param attName
     */
    public void removeAttribute(String attName) {
        if (attName != null && !"".equals(attName.trim())) {
            attributes.remove(attName.toLowerCase());
        }
    }

    /**
     * @return List of child TagNode objects.
     * @deprecated use {@link TagNode#getChildTagList()}, will be refactored and possibly removed in
     *             future versions. TODO This method should be refactored because is does not
     *             properly match the commonly used Java's getter/setter strategy.
     */
    @Deprecated
    public List<TagNode> getChildren() {
        return getChildTagList();
    }

    public void setChildren(List<? extends BaseToken> children) {
    	this.children.clear();
        this.children.addAll(children);
    }

    public List<? extends BaseToken> getAllChildren() {
        return children;
    }

    /**
     * @return List of child TagNode objects.
     */
    public List<TagNode> getChildTagList() {
        List<TagNode> childTagList = new ArrayList<TagNode>();
        for (Object item: children) {
            if (item instanceof TagNode) {
                childTagList.add((TagNode) item);
            }
        }

        return childTagList;
    }

    /**
     * @return Whether this node has child elements or not.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * @return An array of child TagNode instances.
     */
    public TagNode[] getChildTags() {
        List<TagNode> childTagList = getChildTagList();
        TagNode childrenArray[] = new TagNode[childTagList.size()];
        for (int i = 0; i < childTagList.size(); i++) {
            childrenArray[i] = (TagNode) childTagList.get(i);
        }

        return childrenArray;
    }

    /**
     * @return Text content of this node and it's subelements.
     */
    public CharSequence getText() {
        StringBuilder text = new StringBuilder();
        for (Object item :children) {
            if (item instanceof ContentNode) {
                text.append(((ContentNode) item).getContent());
            } else if (item instanceof TagNode) {
                CharSequence subtext = ((TagNode) item).getText();
                text.append(subtext);
            }
        }

        return text;
    }

    /**
     * @param child Child to find index of
     * @return Index of the specified child node inside this node's children, -1 if node is not the
     *         child
     */
    public int getChildIndex(HtmlNode child) {
        int index = 0;
        for (Object curr : children) {
            if (curr == child) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Inserts specified node at specified position in array of children
     *
     * @param index
     * @param childToAdd
     */
    public void insertChild(int index, HtmlNode childToAdd) {
        children.add(index, childToAdd);
    }

    /**
     * Inserts specified node in the list of children before specified child
     *
     * @param node Child before which to insert new node
     * @param nodeToInsert Node to be inserted at specified position
     */
    public void insertChildBefore(HtmlNode node, HtmlNode nodeToInsert) {
        int index = getChildIndex(node);
        if (index >= 0) {
            insertChild(index, nodeToInsert);
        }
    }

    /**
     * Inserts specified node in the list of children after specified child
     *
     * @param node Child after which to insert new node
     * @param nodeToInsert Node to be inserted at specified position
     */
    public void insertChildAfter(HtmlNode node, HtmlNode nodeToInsert) {
        int index = getChildIndex(node);
        if (index >= 0) {
            insertChild(index + 1, nodeToInsert);
        }
    }

    /**
     * @return Parent of this node, or null if this is the root node.
     */
    public TagNode getParent() {
        return parent;
    }

    public DoctypeToken getDocType() {
        return docType;
    }

    public void setDocType(DoctypeToken docType) {
        this.docType = docType;
    }

    public void addChild(Object child) {
        if (child == null) {
            return;
        }
        if (child instanceof List) {
            addChildren((List) child);
        } else if (child instanceof ProxyTagNode) {
            children.add(((ProxyTagNode) child).getToken());
        } else if (child instanceof BaseToken){
            children.add((BaseToken)child);
            if (child instanceof TagNode) {
                TagNode childTagNode = (TagNode) child;
                childTagNode.parent = this;
            }
        } else {
        	throw new RuntimeException("Attempted to add invalid child object to TagNode; class="+child.getClass());
        }
    }

    /**
     * Add all elements from specified list to this node.
     *
     * @param newChildren
     */
    public void addChildren(List<? extends BaseToken> newChildren) {
        if (newChildren != null) {
            for (Object child: newChildren) {
                addChild(child);
            }
        }
    }

    /**
     * Finds first element in the tree that satisfy specified condition.
     *
     * @param condition
     * @param isRecursive
     * @return First TagNode found, or null if no such elements.
     */
    private TagNode findElement(ITagNodeCondition condition, boolean isRecursive) {
        if (condition != null) {
            for (Object item : children) {
                if (item instanceof TagNode) {
                    TagNode currNode = (TagNode) item;
                    if (condition.satisfy(currNode)) {
                        return currNode;
                    } else if (isRecursive) {
                        TagNode inner = currNode.findElement(condition, isRecursive);
                        if (inner != null) {
                            return inner;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Get all elements in the tree that satisfy specified condition.
     * @param condition
     * @param isRecursive
     * @return List of TagNode instances.
     */
    private List<TagNode> findMatchingTagNodes(ITagNodeCondition condition, boolean isRecursive){
        List<TagNode> result = new LinkedList<TagNode>();
        if (condition == null) {
            return result;
        }

        for (Object item : children) {
            if (item instanceof TagNode) {
                TagNode currNode = (TagNode) item;
                if (condition.satisfy(currNode)) {
                    result.add(currNode);
                }
                if (isRecursive) {
                    List<TagNode> innerList = currNode.findMatchingTagNodes(condition, isRecursive);
                    if (innerList != null && innerList.size() > 0) {
                        result.addAll(innerList);
                    }
                }
            }
        }

        return result;	
    }

    /**
     * Get all elements in the tree that satisfy specified condition.
     *
     * @param condition
     * @param isRecursive
     * @return List of TagNode instances with specified name.
     */
    public List<? extends TagNode> getElementList(ITagNodeCondition condition, boolean isRecursive) {
        return findMatchingTagNodes(condition, isRecursive);
    }

    /**
     * @param condition
     * @param isRecursive
     * @return The array of all subelements that satisfy specified condition.
     */
    private TagNode[] getElements(ITagNodeCondition condition, boolean isRecursive) {
        final List<TagNode> list = findMatchingTagNodes(condition, isRecursive);
        TagNode array[];
        if (list == null) {
            array = new TagNode[0];
        } else {
            array = (TagNode[]) list.toArray(new TagNode[list.size()]);
        }
        return array;
    }

    public List<? extends TagNode> getAllElementsList(boolean isRecursive) {
        return getElementList(new TagAllCondition(), isRecursive);
    }

    public TagNode[] getAllElements(boolean isRecursive) {
        return getElements(new TagAllCondition(), isRecursive);
    }

    public TagNode findElementByName(String findName, boolean isRecursive) {
        return findElement(new TagNodeNameCondition(findName), isRecursive);
    }

    public List<? extends TagNode> getElementListByName(String findName, boolean isRecursive) {
        return getElementList(new TagNodeNameCondition(findName), isRecursive);
    }

    public TagNode[] getElementsByName(String findName, boolean isRecursive) {
        return getElements(new TagNodeNameCondition(findName), isRecursive);
    }

    public TagNode findElementHavingAttribute(String attName, boolean isRecursive) {
        return findElement(new TagNodeAttExistsCondition(attName), isRecursive);
    }

    public List<? extends TagNode> getElementListHavingAttribute(String attName, boolean isRecursive) {
        return getElementList(new TagNodeAttExistsCondition(attName), isRecursive);
    }

    public TagNode[] getElementsHavingAttribute(String attName, boolean isRecursive) {
        return getElements(new TagNodeAttExistsCondition(attName), isRecursive);
    }

    public TagNode findElementByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) {
        return findElement(new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive);
    }

    public List<? extends TagNode> getElementListByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) {
        return getElementList(new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive);
    }

    public TagNode[] getElementsByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) {
        return getElements(new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive);
    }

    /**
     * Evaluates XPath expression on give node. <br>
     * <em>
     *  This is not fully supported XPath parser and evaluator.
     *  Examples below show supported elements:
     * </em> <code>
     * <ul>
     *      <li>//div//a</li>
     *      <li>//div//a[@id][@class]</li>
     *      <li>/body/*[1]/@type</li>
     *      <li>//div[3]//a[@id][@href='r/n4']</li>
     *      <li>//div[last() >= 4]//./div[position() = last()])[position() > 22]//li[2]//a</li>
     *      <li>//div[2]/@*[2]</li>
     *      <li>data(//div//a[@id][@class])</li>
     *      <li>//p/last()</li>
     *      <li>//body//div[3][@class]//span[12.2<position()]/@id</li>
     *      <li>data(//a['v' < @id])</li>
     * </ul>
     * </code>
     *
     * @param xPathExpression
     * @return result of XPather evaluation.
     * @throws XPatherException
     */
    public Object[] evaluateXPath(String xPathExpression) throws XPatherException {
        return new XPather(xPathExpression).evaluateAgainstNode(this);
    }

    /**
     * Remove this node from the tree.
     *
     * @return True if element is removed (if it is not root node).
     */
    public boolean removeFromTree() {
        return parent != null ? parent.removeChild(this) : false;
    }

    /**
     * Remove specified child element from this node.
     *
     * @param child
     * @return True if child object existed in the children list.
     */
    public boolean removeChild(Object child) {
        return this.children.remove(child);
    }

    /**
     * Removes all children (subelements and text content).
     */
    public void removeAllChildren() {
        this.children.clear();
    }

    void addItemForMoving(Object item) {
        if (itemsToMove == null) {
            itemsToMove = new ArrayList<BaseToken>();
        }
        if (item instanceof BaseToken){
            itemsToMove.add((BaseToken)item);        	
        } else {
        	throw new RuntimeException("Attempt to add invalid item for moving; class="+item.getClass());
        }

    }

    List<? extends BaseToken> getItemsToMove() {
        return itemsToMove;
    }

    void setItemsToMove(List<BaseToken> itemsToMove) {
        this.itemsToMove = itemsToMove;
    }

    boolean isFormed() {
        return isFormed;
    }

    void setFormed(boolean isFormed) {
        this.isFormed = isFormed;
    }

    void setFormed() {
        setFormed(true);
    }

    /**
     * @param autoGenerated the autoGenerated to set
     */
    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    /**
     * @return the autoGenerated
     */
    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    /**
     * @return true, if node was marked to be pruned.
     */
    public boolean isPruned() {
        return pruned;
    }

    public void setPruned(boolean pruned) {
        this.pruned = pruned;
    }

    public boolean isEmpty() {
        if (!isPruned()) {
            for (Object child : this.children) {
                if (child instanceof TagNode) {
                    if (!((TagNode) child).isPruned()) {
                        return false;
                    }
                } else if (child instanceof ContentNode) {
                    if (!((ContentNode) child).isBlank()) {
                        return false;
                    }
                } else if (child instanceof CommentNode) {
                    // ideally could be discarded - however standard practice is to include browser specific commands in comments. :-(
                    return false;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Adds namespace declaration to the node
     *
     * @param nsPrefix Namespace prefix
     * @param nsURI Namespace URI
     */
    public void addNamespaceDeclaration(String nsPrefix, String nsURI) {
        if (nsDeclarations == null) {
            nsDeclarations = new TreeMap<String, String>();
        }
        nsDeclarations.put(nsPrefix, nsURI);
    }

    /**
     * Collect all prefixes in namespace declarations up the path to the document root from the
     * specified node
     *
     * @param prefixes Set of prefixes to be collected
     */
    void collectNamespacePrefixesOnPath(Set<String> prefixes) {
        Map<String, String> nsDeclarations = getNamespaceDeclarations();
        if (nsDeclarations != null) {
            for (String prefix : nsDeclarations.keySet()) {
                prefixes.add(prefix);
            }
        }
        if (parent != null) {
            parent.collectNamespacePrefixesOnPath(prefixes);
        }
    }

    String getNamespaceURIOnPath(String nsPrefix) {
        if (nsDeclarations != null) {
            for (Map.Entry<String, String> nsEntry : nsDeclarations.entrySet()) {
                String currName = nsEntry.getKey();
                if (currName.equals(nsPrefix) || ("".equals(currName) && nsPrefix == null)) {
                    return nsEntry.getValue();
                }
            }
        }
        if (parent != null) {
            return parent.getNamespaceURIOnPath(nsPrefix);
        }

        return null;
    }

    /**
     * @return Map of namespace declarations for this node
     */
    public Map<String, String> getNamespaceDeclarations() {
        return nsDeclarations;
    }

    public void serialize(Serializer serializer, Writer writer) throws IOException {
        serializer.serialize(this, writer);
    }

    public TagNode makeCopy() {
        TagNode copy = new TagNode(name, true);
        copy.attributes.putAll(attributes);
        return copy;
    }

    public boolean isCopy() {
        return isCopy;
    }

    /**
     * Traverses the tree and performs visitor's action on each node. It stops when it finishes all
     * the tree or when visitor returns false.
     *
     * @param visitor TagNodeVisitor implementation
     */
    public void traverse(TagNodeVisitor visitor) {
        traverseInternally(visitor);
    }

    private boolean traverseInternally(TagNodeVisitor visitor) {
        if (visitor != null) {
            boolean hasParent = parent != null;
            boolean toContinue = visitor.visit(parent, this);

            if (!toContinue) {
                return false; // if visitor stops traversal
            } else if (hasParent && parent == null) {
                return true; // if this node is pruned from the tree during the visit, then don't go deeper
            }
            for (Object child : children.toArray()) { // make an array to avoid ConcurrentModificationException when some node is cut
                if (child instanceof TagNode) {
                    toContinue = ((TagNode) child).traverseInternally(visitor);
                } else if (child instanceof ContentNode) {
                    toContinue = visitor.visit(this, (ContentNode) child);
                } else if (child instanceof CommentNode) {
                    toContinue = visitor.visit(this, (CommentNode) child);
                }
                if (!toContinue) {
                    return false;
                }
            }
        }
        return true;
    }

	/**
	 * @return the isForeignMarkup
	 */
	public boolean isForeignMarkup() {
		return isForeignMarkup;
	}

	/**
	 * @param isForeignMarkup the isForeignMarkup to set
	 */
	public void setForeignMarkup(boolean isForeignMarkup) {
		foreignMarkupFlagSet = true;
		this.isForeignMarkup = isForeignMarkup;
		
		//
		// if set to false, change all existing attributes of this
		// element to lowercase.
		//
		if (!isForeignMarkup){
			this.replaceAttributes(getAttributesInLowerCase());
		}
	}
	
	/**
	 * Returns a copy of the set of attributes for this node with lowercase
	 * names
	 * @return a map of attributes in key/value pairs with names in lowercase
	 */
	private Map<String, String> attributesToLowerCase(){
		Map<String, String> lowerCaseAttributes = new LinkedHashMap<String, String>();
		for (String key: attributes.keySet()){
			lowerCaseAttributes.put(key.toLowerCase(), attributes.get(key));
		}
		return lowerCaseAttributes;
	}

}