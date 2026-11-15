// Deliberately small text grammar. All output remains Vue text nodes; raw HTML is never parsed.
export function sellerLink(value) {
    if(typeof value!=='string'||value.length>2000||/[\s\u0000-\u001f\u007f]/.test(value))return null;
    try {const url=new URL(value);return ['https:','http:'].includes(url.protocol)&&!url.username&&!url.password?url.href:null;}catch{return null;}
}
export function inlineSellerText(text, formatted=false) {
    if(!formatted)return [{kind:'text',text}];
    const tokens=[],pattern=/(\*\*[^*\n]+\*\*|\*[^*\n]+\*|\[[^\]\n]{1,300}\]\([^)\s]{1,2000}\))/g;
    let offset=0;
    for(const match of text.matchAll(pattern)){
        if(match.index>offset)tokens.push({kind:'text',text:text.slice(offset,match.index)});
        const part=match[0];
        if(part.startsWith('**'))tokens.push({kind:'bold',text:part.slice(2,-2)});
        else if(part.startsWith('*'))tokens.push({kind:'italic',text:part.slice(1,-1)});
        else {const link=part.match(/^\[([^\]]+)\]\((.+)\)$/),href=sellerLink(link[2]);tokens.push(href?{kind:'link',text:link[1],href}:{kind:'text',text:part});}
        offset=match.index+part.length;
    }
    if(offset<text.length)tokens.push({kind:'text',text:text.slice(offset)});
    return tokens;
}
export function sellerTextBlocks(text, formatted=false) {
    const result=[];let pending=[];
    const flush=()=>{if(pending.length)result.push({kind:'paragraph',text:pending.join('\n')});pending=[];};
    for(const line of text.replace(/\r\n?/g,'\n').split('\n')){
        if(!line.trim()){flush();continue;}
        const heading=line.match(formatted?/^\s*(#{2,3})\s+(.+)$/:/^\s*(?:#{1,3}\s+(.+)|\*\*(.+)\*\*)\s*$/);
        if(heading){flush();result.push({kind:'heading',level:formatted?heading[1].length:3,text:formatted?heading[2]:heading[1]||heading[2]});continue;}
        const bullet=line.match(/^\s*[-*]\s+(.+)$/),numbered=formatted&&line.match(/^\s*\d+[.]\s+(.+)$/);
        if(bullet||numbered){flush();const kind=numbered?'ordered':'list',last=result.at(-1),item=(numbered||bullet)[1];if(last?.kind===kind)last.items.push(item);else result.push({kind,items:[item]});}
        else pending.push(line);
    }
    flush();return result;
}
