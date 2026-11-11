package com.bonsaimarket.backend.support;

import java.util.regex.Pattern;

/** Translates UPDATE JOIN to correlated UPDATE without removing predicates or constraint checks. */
public final class H2Sql {
    public static String joins(String script){
        script=script.replaceAll("(?m)^--[^\\n]*","");
        var statements=new StringBuilder();var part=new StringBuilder();boolean quoted=false;
        for(int i=0;i<script.length();i++){
            char ch=script.charAt(i);part.append(ch);
            if(ch=='\''){if(quoted&&i+1<script.length()&&script.charAt(i+1)=='\''){part.append(script.charAt(++i));}else quoted=!quoted;}
            if(ch==';'&&!quoted){statements.append(statement(part.substring(0,part.length()-1))).append(';');part.setLength(0);}
        }
        return statements.append(part).toString();
    }
    private static String statement(String source){
        String sql=source.replaceAll("(?m)^--[^\\n]*","").trim();
        var alter=Pattern.compile("(?is)^ALTER\\s+TABLE\\s+(\\w+)\\s+(.*)$").matcher(sql);
        if(alter.matches()){
            var actions=topLevel(alter.group(2));
            if(actions.size()>1)return actions.stream().map(a->"ALTER TABLE "+alter.group(1)+" "+a).collect(java.util.stream.Collectors.joining(";\n"));
        }
        var join=Pattern.compile("(?is)^UPDATE\\s+(\\w+)\\s+(\\w+)\\s+JOIN\\s+(.*?)\\s+SET\\s+(.*?)(?:\\s+WHERE\\s+(.*))?$").matcher(sql);
        if(!join.matches())return source;
        String table=join.group(1),alias=join.group(2),from=join.group(3),set=join.group(4),where=join.group(5)==null?"1=1":join.group(5);
        // Move the first ON predicate into the correlated WHERE; remaining JOINs retain their ON clauses.
        var first=Pattern.compile("(?is)^(.*?)\\s+ON\\s+(.*?)(\\s+JOIN\\s+.*|$)").matcher(from);
        if(!first.matches())throw new IllegalArgumentException("Unsupported test UPDATE JOIN");
        from=first.group(1)+first.group(3);where=first.group(2)+" AND ("+where+")";
        // Assignments may contain commas inside strings or CASE expressions only in the known seed format.
        var fields=new java.util.ArrayList<String>();var values=new java.util.ArrayList<String>();
        for(String assignment:set.split(",(?=\\s*"+alias+"[.])")){
            int eq=assignment.indexOf('=');fields.add(assignment.substring(0,eq).trim().replace(alias+".",""));values.add(assignment.substring(eq+1).trim());
        }
        return "UPDATE "+table+" "+alias+" SET ("+String.join(",",fields)+")=(SELECT "+String.join(",",values)+" FROM "+from+" WHERE "+where+") WHERE EXISTS (SELECT 1 FROM "+from+" WHERE "+where+")";
    }
    private static java.util.List<String> topLevel(String input){
        var result=new java.util.ArrayList<String>();int depth=0,start=0;boolean quoted=false;
        for(int i=0;i<input.length();i++){char c=input.charAt(i);if(c=='\''){if(quoted&&i+1<input.length()&&input.charAt(i+1)=='\'')i++;else quoted=!quoted;}else if(!quoted){if(c=='(')depth++;if(c==')')depth--;if(c==','&&depth==0){result.add(input.substring(start,i).strip());start=i+1;}}}
        result.add(input.substring(start).strip());return result;
    }
}
