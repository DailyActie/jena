/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.lib;

import static com.hp.hpl.jena.tdb.sys.SystemTDB.LenNodeHash;
import static com.hp.hpl.jena.tdb.sys.SystemTDB.SizeOfLong;

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import lib.Bytes;
import lib.Tuple;

import com.hp.hpl.jena.rdf.model.AnonId;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.shared.PrefixMapping;

import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.QuadPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.sse.SSEParseException;
import com.hp.hpl.jena.sparql.util.ALog;
import com.hp.hpl.jena.sparql.util.FmtUtils;

import com.hp.hpl.jena.tdb.TDBException;
import com.hp.hpl.jena.tdb.base.record.Record;
import com.hp.hpl.jena.tdb.base.record.RecordFactory;
import com.hp.hpl.jena.tdb.pgraph.Hash;
import com.hp.hpl.jena.tdb.pgraph.NodeId;
import com.hp.hpl.jena.tdb.pgraph.NodeType;

import dev.idx2.ColumnMap;

public class NodeLib
{
    public static String encode(Node node)  { return encode(node, null) ; }

    public static String encode(Node node, PrefixMapping pmap)
    {
        if ( node.isBlank() )
            return "_:"+node.getBlankNodeLabel() ;
        return FmtUtils.stringForNode(node, pmap) ;
    }

    public static Node decode(String s)     { return decode(s, null) ; }
    
    public static Node decode(String s, PrefixMapping pmap)
    {
        if ( s.startsWith("_:") )   
        {
            s = s.substring(2) ;
            return Node.createAnon(new AnonId(s)) ;
        }
        
        try {
            return SSE.parseNode(s, pmap) ;
        } catch (SSEParseException ex)
        {
            ALog.fatal(NodeLib.class, "decode: Failed to parse: "+s) ;
            throw ex ;
        }
    }
    
//    /** Canonical language tag : RFC 3066 and RFC 2234 */
//    public String langTag(String langTag)
//    {
//        
//    }
    
    /** Get the triples in the form of a List<Triple> */
    public static List<Triple> tripleList(OpBGP opBGP)
    {
        return tripleList(opBGP.getPattern()) ;
    }
    
    /** Get the triples in the form of a List<Triple> */
    public static List<Triple> tripleList(BasicPattern pattern)
    {
        return tripleList(pattern.getList()) ;
    }
    
    /** Cast a list (known to be triples, e.g. from Java 1.4) to a List<Triple> */
    public static List<Triple> tripleList(List<?> triples)
    {
        @SuppressWarnings("unchecked")
        List<Triple> x = (List<Triple>)triples ;
        return x ;
    }
    
    /** Get the triples in the form of a List<Triple> */
    public static List<Quad> quadList(OpQuadPattern opQuad)
    {
        return quadList(opQuad.getQuads()) ;
    }
    
    /** Get the triples in the form of a List<Triple> */
    public static List<Quad> quadList(QuadPattern pattern)
    {
        return quadList(pattern.getList()) ;
    }

    /** Cast a list (known to be triples, e.g. from Java 1.4) to a List<Triple> */
    public static List<Quad> quadList(List<?> quads)
    {
        @SuppressWarnings("unchecked")
        List<Quad> x = (List<Quad>)quads ;
        return x ;
    }
    
    /** Cast a list (known to be nodes, e.g. from Java 1.4) to a List<Node> */
    public static List<Node> nodeList(List<?> nodes)
    {
        @SuppressWarnings("unchecked")
        List<Node> x = (List<Node>)nodes ;
        return x ;
    }
    
    /** Cast a list (known to be vars, e.g. from Java 1.4) to a List<Var> */
    public static List<Var> varList(List<?> vars)
    {
        @SuppressWarnings("unchecked")
        List<Var> x = (List<Var>)vars ;
        return x ;
    }
    
    public static Hash hash(Node n)
    { 
        Hash h = new Hash(LenNodeHash) ;
        setHash(h, n) ;
        return h ;
    }
    
    public static void setHash(Hash h, Node n) 
    {
        NodeType nt = NodeType.lookup(n) ;
        switch(nt) 
        {
            case URI:
                hash(h, n.getURI(), null, null, nt) ;
                return ;
            case BNODE:
                hash(h, n.getBlankNodeLabel(), null, null, nt) ;
                return ;
            case LITERAL:
                hash(h,
                     n.getLiteralLexicalForm(),
                     n.getLiteralLanguage(),
                     n.getLiteralDatatypeURI(), nt) ;
                return  ;
            case OTHER:
                throw new TDBException("Attempt to hash something strange: "+n) ; 
        }
        throw new TDBException("NodeType broken: "+n) ; 
    }
    
    private static void hash(Hash h, String lex, String lang, String datatype, NodeType nodeType)
    {
        if ( datatype == null )
            datatype = "" ;
        if ( lang == null )
            lang = "" ;
        String toHash = lex + "|" + lang + "|" + datatype+"|"+nodeType.getName() ;
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            digest.update(toHash.getBytes("UTF8"));
            if ( h.getLen() == 16 )
                // MD5 is 16 bytes.
                digest.digest(h.getBytes(), 0, 16) ;
            else
            {
                byte b[] = digest.digest(); // 16 bytes.
                // Avoid the copy? If length is 16.  digest.digest(bytes, 0, length) needs 16 bytes
                System.arraycopy(b, 0, h.getBytes(), 0, h.getLen()) ;
            }
            return ;
        }
        catch (NoSuchAlgorithmException e)
        { e.printStackTrace(); }
        catch (UnsupportedEncodingException e)
        { e.printStackTrace(); } 
        catch (DigestException ex)
        { ex.printStackTrace(); }
        return ;
    }
    
    public static NodeId getNodeId(Record r, int idx)
    {
        return NodeId.create(Bytes.getLong(r.getKey(), idx)) ;
    }
    
    public static Tuple<NodeId> tuple(Record r, ColumnMap cMap)
    {
        int N = r.getKey().length/SizeOfLong ;
        NodeId[] nodeIds = new NodeId[N] ;
        for ( int i = 0 ; i < N ; i++ )
        {
            long x = Bytes.getLong(r.getKey(), i*SizeOfLong) ;
            NodeId id = NodeId.create(x) ;
            int j = i ;
            if ( cMap != null )
                j = cMap.fetchSlotIdx(i) ;
            nodeIds[j] = id ;
        }
        return new Tuple<NodeId>(nodeIds) ;
    }
    
    public static Record record(RecordFactory factory, Tuple<NodeId> tuple, ColumnMap cMap) 
    {
        byte[] b = new byte[tuple.size()*NodeId.SIZE] ;
        for ( int i = 0 ; i < tuple.size() ; i++ )
        {
            int j = cMap.mapSlotIdx(i) ;
            // i'th Nodeid goes to j'th bytes slot.
            Bytes.setLong(tuple.get(i).getId(), b,j*SizeOfLong) ;
        }
            
        return factory.create(b) ;
    }

    // OLD to go.
    @Deprecated
    public static Record record(RecordFactory factory, NodeId...nodeIds)
    {
        byte[] b = new byte[nodeIds.length*NodeId.SIZE] ;
        for ( int i = 0 ; i < nodeIds.length ; i++ )
            Bytes.setLong(nodeIds[i].getId(), b, i*SizeOfLong) ;  
        return factory.create(b) ;
    }
    
    // OLD to go.
    @Deprecated
    public static Record record(RecordFactory factory, long...nodeIds)
    {
        byte[] b = new byte[nodeIds.length*NodeId.SIZE] ;
        for ( int i = 0 ; i < nodeIds.length ; i++ )
            Bytes.setLong(nodeIds[i], b, i*SizeOfLong) ;  
        return factory.create(b) ;
    }

    public static Node termOrAny(Node node)
    {
        if ( node == null || node.isVariable() )
            return Node.ANY ;
        return node ;
    }
}

/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */