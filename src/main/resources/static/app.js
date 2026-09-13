'use strict';
const el=id=>document.getElementById(id);
let pendente=null, mensagemVoz='', ocupado=false, gravador=null, fluxoMicrofone=null, limiteGravacao=null, partes=[];
const moeda=v=>new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(v);
async function api(url,options={}){
  const response=await fetch(url,options);
  if(!response.ok){let erro={};try{erro=await response.json();}catch{} throw new Error(erro.mensagem||erro.erro||`Falha HTTP ${response.status}`);}
  return response.json();
}
const json=body=>({method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
async function tarefa(acao){
  if(ocupado)return;
  ocupado=true;el('estado').textContent='Processando localmente. Aguarde…';
  document.querySelectorAll('button').forEach(b=>b.disabled=true);
  try{await acao();el('estado').textContent='Concluído.';}catch(e){el('estado').textContent=e.message;}
  finally{ocupado=false;document.querySelectorAll('button').forEach(b=>b.disabled=false);el('parar').disabled=true;}
}
function mostrar(resposta,transcricao='',audio=null){
  pendente=resposta.acao==='CONFIRMACAO_NECESSARIA'?resposta.dados.id:null;
  mensagemVoz=resposta.mensagem;el('resultado').hidden=false;
  el('mensagem').textContent=resposta.mensagem;
  el('transcricao').textContent=transcricao?`Transcrição automática — confira: ${transcricao}`:'';
  el('dados').textContent=resposta.dados?JSON.stringify(resposta.dados,null,2):'';
  el('confirmar').hidden=!pendente;el('ouvir').hidden=false;
  el('voz').pause();if(el('voz').src.startsWith('blob:'))URL.revokeObjectURL(el('voz').src);
  el('voz').removeAttribute('src');el('voz').hidden=!audio;
  if(audio)el('voz').src=`data:audio/wav;base64,${audio}`;
}
async function atualizar(){
  const mes=el('mes').value;
  const [r,itens]=await Promise.all([api(`/api/resumo?mes=${encodeURIComponent(mes)}`),api(`/api/lancamentos?mes=${encodeURIComponent(mes)}`)]);
  el('receitas').textContent=moeda(r.receitas);el('despesas').textContent=moeda(r.despesas);el('saldo').textContent=moeda(r.saldo);
  el('comprometimento').textContent=r.percentualDividasSobreReceitas==null?'Não calculado':`${r.percentualDividasSobreReceitas.toFixed(2)}%`;
  el('lancamentos').replaceChildren();
  for(const item of itens){const tr=document.createElement('tr');for(const valor of [item.data,item.tipo,item.categoria,item.descricao,moeda(item.valor)]){const td=document.createElement('td');td.textContent=valor;tr.append(td);}el('lancamentos').append(tr);}
  if(!itens.length){const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=5;td.textContent='Nenhum lançamento confirmado neste mês.';tr.append(td);el('lancamentos').append(tr);}
}
el('enviar').onclick=()=>tarefa(async()=>{const r=await api('/api/assistente/texto',json({texto:el('comando').value}));mostrar(r);if(r.acao==='RESUMO'){el('mes').value=r.dados.mes;await atualizar();}});
el('consultar').onclick=()=>tarefa(atualizar);
el('confirmar').onclick=()=>tarefa(async()=>{
  if(!pendente)return;const id=pendente;
  const r=await api(`/api/lancamentos/${id}/confirmar`,{method:'POST'});
  mostrar({acao:'CONFIRMADO',mensagem:'Lançamento confirmado. Repetir a confirmação deste mesmo ID não cria outra transação.',dados:r});
  el('mes').value=r.data.slice(0,7);await atualizar();
});
el('manual').onsubmit=e=>{e.preventDefault();tarefa(async()=>{
  const dados=Object.fromEntries(new FormData(e.target)); // Valor decimal enviado como string, sem calculo em JS.
  const r=await api('/api/lancamentos/rascunhos',json(dados));
  mostrar({acao:'CONFIRMACAO_NECESSARIA',mensagem:'Confira todos os campos antes de confirmar.',dados:r});
});};
el('ouvir').onclick=()=>tarefa(async()=>{
  const response=await fetch('/api/audio/voz',json({texto:mensagemVoz}));
  if(!response.ok)throw new Error('Voz local indisponível. Verifique o serviço de áudio.');
  if(el('voz').src.startsWith('blob:'))URL.revokeObjectURL(el('voz').src);
  el('voz').src=URL.createObjectURL(await response.blob());el('voz').hidden=false;await el('voz').play();
});
async function processarAudio(arquivo){
  if(!arquivo||!arquivo.size||arquivo.size>10*1024*1024)throw new Error('Escolha um áudio de até 10 MB.');
  const form=new FormData();form.append('arquivo',arquivo,'comando.webm');
  const r=await api('/api/assistente/audio',{method:'POST',body:form});
  mostrar(r.resposta,r.transcricao,r.audioBase64);
  if(r.avisoAudio)el('mensagem').textContent+=` ${r.avisoAudio}`;
  if(r.resposta.acao==='RESUMO'){el('mes').value=r.resposta.dados.mes;await atualizar();}
}
el('enviarArquivo').onclick=()=>tarefa(()=>processarAudio(el('arquivo').files[0]));
el('gravar').onclick=async()=>{
  if(ocupado)return;
  try{
    fluxoMicrofone=await navigator.mediaDevices.getUserMedia({audio:true});
    partes=[];gravador=new MediaRecorder(fluxoMicrofone);
    gravador.ondataavailable=e=>{if(e.data.size)partes.push(e.data);};
    gravador.onstop=()=>{clearTimeout(limiteGravacao);fluxoMicrofone.getTracks().forEach(t=>t.stop());ocupado=false;tarefa(()=>processarAudio(new Blob(partes,{type:gravador.mimeType})));};
    gravador.start();ocupado=true;document.querySelectorAll('button').forEach(b=>b.disabled=true);el('parar').disabled=false;
    el('estado').textContent='Gravando. Use somente dados fictícios. Limite de 60 segundos.';
    limiteGravacao=setTimeout(()=>{if(gravador.state==='recording')gravador.stop();},60000);
  }catch(e){if(fluxoMicrofone)fluxoMicrofone.getTracks().forEach(t=>t.stop());el('estado').textContent='Microfone indisponível. Você pode enviar um arquivo ou usar texto.';}
};
el('parar').onclick=()=>{if(gravador&&gravador.state==='recording')gravador.stop();};
atualizar().catch(e=>el('estado').textContent=e.message);
