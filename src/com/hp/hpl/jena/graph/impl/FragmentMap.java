/*
  (c) Copyright 2002, Hewlett-Packard Company, all rights reserved.
  [See end of file]
  $Id: FragmentMap.java,v 1.2 2003-05-30 13:50:12 chris-dollin Exp $
*/

package com.hp.hpl.jena.graph.impl;

import java.util.HashMap;

import com.hp.hpl.jena.graph.*;

/**
    a FragmentMap is a Map where the domain elements are Nodes
    and the range elements are Triples or Fragments. The specialised
    put methods return the range element that has been put, because
    the context of use is usually of the form:
<p>    
    return map.putThingy( node, fragmentExpression )
<p>
    @author kers
*/

public class FragmentMap extends HashMap
    {
    public FragmentMap() { super(); }
    
    /**
        update the map with (node -> triple); return the triple
    */
    public Triple putTriple( Node key, Triple value )
        {
        put( key, value );
        return value;
        }
        
    /**
        update the map with (node -> fragment); return the fragment.
    */
    public Fragments putFragments( Node key, Fragments value )
        {
        put( key, value );
        return value;
        }        
    }

/*
    (c) Copyright Hewlett-Packard Company 2003
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/