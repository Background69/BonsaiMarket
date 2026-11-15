export const potShapes={ROUND:'Tròn',RECTANGULAR:'Chữ nhật',SQUARE:'Vuông',OVAL:'Bầu dục',OTHER:'Khác'};
const number=value=>typeof value==='number'&&Number.isFinite(value)&&value>0;
const measure=value=>new Intl.NumberFormat('vi-VN',{maximumFractionDigits:1}).format(value);
export function potSpecificationRows(specs){
    if(!specs)return [];
    const rows=[],add=(label,value)=>{if(value!==null&&value!==undefined&&String(value).trim())rows.push({label,value});};
    add('Chất liệu',specs.material);add('Kiểu dáng',potShapes[specs.shape]);add('Màu sắc',specs.color);
    for(const [key,label] of [['diameterCm','Đường kính'],['lengthCm','Chiều dài'],['widthCm','Chiều rộng'],['heightCm','Chiều cao chậu']])if(number(specs[key]))add(label,measure(specs[key])+' cm');
    add('Lỗ thoát nước',{YES:'Có',NO:'Không',UNKNOWN:'Chưa rõ'}[specs.drainageStatus]);
    if(Number.isSafeInteger(specs.drainageHoleCount)&&specs.drainageHoleCount>=0)add('Số lỗ thoát nước',specs.drainageHoleCount+' lỗ');
    if(number(specs.weightGrams))add('Khối lượng',specs.weightGrams>=1000?measure(specs.weightGrams/1000)+' kg':measure(specs.weightGrams)+' g');
    if(typeof specs.hasTray==='boolean')add('Kèm khay lót',specs.hasTray?'Có':'Không');
    add('Phù hợp loại cây',specs.suitablePlants);add('Hướng dẫn sử dụng',specs.usageInstructions);return rows;
}
export function potPurchaseHighlights(specs){
    if(!specs)return [];
    const rows=potSpecificationRows(specs),labels=['Chất liệu','Kiểu dáng'];
    const result=rows.filter(row=>labels.includes(row.label));
    if(['RECTANGULAR','SQUARE'].includes(specs.shape)&&number(specs.lengthCm)&&number(specs.widthCm))result.push({label:'Dài × rộng',value:measure(specs.lengthCm)+' × '+measure(specs.widthCm)+' cm'});
    else if(number(specs.diameterCm))result.push({label:'Đường kính',value:measure(specs.diameterCm)+' cm'});
    result.push(...rows.filter(row=>['Chiều cao chậu','Lỗ thoát nước'].includes(row.label)));return result.slice(0,5);
}
