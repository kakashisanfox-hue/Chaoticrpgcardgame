package com.chaotic.cardrpg

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.graphics.*
import android.graphics.BitmapFactory
import android.view.*
import android.widget.EditText
import android.app.AlertDialog
import kotlin.math.*
import kotlin.random.Random

class MainActivity: Activity() {
 private lateinit var game: GameView
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; game=GameView(); setContentView(game) }
 private data class UnitCard(val name:String,var hp:Int,val atk:Int,var used:Boolean=false)
    inner class GameView: View(this) {
  private val paint=Paint(3); private val rect=RectF(); private val hit=mutableListOf<Pair<RectF,()->Unit>>()
  private var origin=0; private var face=2; private var hair=1; private var skin=0; private var eyes=0; private var outfit=0; private var hairColor=0; private var page="menu"; private var player="Duelista"; private var style=0; private var scene=0; private var turn=1; private var chosen=-1; private var attacker=-1; private var message="Selecione uma carta da mão"; private var defeated=0; private var lost=0

  private val allies=arrayOfNulls<UnitCard>(5); private val foes=arrayOfNulls<UnitCard>(5); private val hand=mutableListOf<UnitCard>(); private var reserve=mutableListOf<UnitCard>()
  private val names=listOf("Maxxor","Intress","Toborn","Blazier","Frafdo","Heptadd","Najarin","Owis")
  private val dialogues=listOf("Tom: Ei! Você é novo por aqui?", "Você: Onde estou? Esse lugar é o Chaotic?", "Kaz: Exatamente! Quer aprender a duelar?", "Tom: Escolha suas cartas e prepare o campo.", "Kaz: Vamos ver se você consegue me vencer!")
  private fun color(s:String)=Color.parseColor(s)
  private fun drawBox(x:Float,y:Float,w:Float,h:Float,fill:Int,stroke:Int=color("#4FB5E8")) {rect.set(x,y,x+w,y+h);paint.color=fill;paint.style=Paint.Style.FILL;canvasRef.drawRoundRect(rect,12f,12f,paint);paint.color=stroke;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;canvasRef.drawRoundRect(rect,12f,12f,paint);paint.style=Paint.Style.FILL}
  private lateinit var canvasRef:Canvas
  private val menuArt:Bitmap? by lazy { loadArt("menu_v02") }
  private val creatorArt:Bitmap? by lazy { loadArt("creator_v02") }
  private fun loadArt(name:String):Bitmap? { val id=resources.getIdentifier(name,"drawable",packageName);return if(id==0)null else try { BitmapFactory.decodeResource(resources,id) }catch(_:Exception){null} }
  private fun art(bitmap:Bitmap?) {if(bitmap!=null){paint.color=Color.WHITE;paint.alpha=255;canvasRef.drawBitmap(bitmap,null,RectF(0f,0f,960f,540f),paint)}}
  private fun hotspot(x:Float,y:Float,w:Float,h:Float,fn:()->Unit){hit.add(RectF(x,y,x+w,y+h) to fn)}
  private fun editName(){val input=EditText(this@MainActivity);input.setText(player);AlertDialog.Builder(this@MainActivity).setTitle("Nome do duelista").setView(input).setPositiveButton("Salvar"){_,_->player=input.text.toString().take(18).ifBlank{"Duelista"};invalidate()}.setNegativeButton("Cancelar",null).show()}
  private fun menuV02(){art(menuArt)
    hotspot(365f,222f,240f,44f){if(scene>0){page="story";invalidate()}else start()}
    hotspot(365f,270f,240f,40f){start()}
    hotspot(365f,315f,240f,40f){page="story";invalidate()}
    hotspot(365f,359f,240f,40f){page="help";invalidate()}
    hotspot(365f,403f,240f,39f){page="help";invalidate()}
    hotspot(365f,446f,240f,39f){page="help";invalidate()}
  }
  private fun creatorV02(){art(creatorArt)
    // Overlays preserve the supplied illustration and expose actual choices.
    drawBox(34f,329f,245f,30f,color("#081226"),color("#4387B5"));txt(player,45f,350f,20f)
    val origins=listOf("OVERWORLD","UNDERWORLD","M'ARRILLIAN")
    txt("Origem: ${origins[origin]}",34f,507f,16f,color("#80FFB8"))
    hotspot(35f,328f,245f,32f){editName()}
    for(i in 0..2)hotspot(35f+i*85f,385f,80f,60f){origin=i;invalidate()}
    for(i in 0..4){hotspot(720f+i*43f,104f,40f,48f){face=i;invalidate()};hotspot(720f+i*43f,201f,40f,48f){hair=i;invalidate()};hotspot(730f+i*43f,357f,39f,65f){outfit=i;invalidate()}}
    for(i in 0..3)hotspot(785f+i*30f,156f,28f,23f){skin=i;invalidate()}
    for(i in 0..6)hotspot(790f+i*22f,253f,20f,23f){hairColor=i;invalidate()}
    for(i in 0..5)hotspot(793f+i*24f,285f,22f,23f){eyes=i;invalidate()}
    drawBox(710f,310f,232f,30f,color("#081226"),color("#A53040"));txt("R${face+1} C${hair+1} P${skin+1} O${eyes+1} V${outfit+1}",719f,332f,17f)
    hotspot(705f,445f,240f,55f){scene=0;page="story";invalidate()}
  }
  private fun txt(s:String,x:Float,y:Float,size:Float=22f,c:Int=Color.WHITE) {paint.color=c;paint.textSize=size;paint.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD);canvasRef.drawText(s,x,y,paint)}
  private fun button(s:String,x:Float,y:Float,w:Float,h:Float,fn:()->Unit) {drawBox(x,y,w,h,color("#101E35"));txt(s,x+16,y+h*.65f,20f);hit.add(RectF(x,y,x+w,y+h) to fn)}
  private fun start() {page="creator";invalidate()}
  private fun duel() {page="duel";turn=1;chosen=-1;attacker=-1;defeated=0;lost=0;allies.fill(null);foes.fill(null);reserve=MutableList(12){makeCard()};hand.clear();repeat(5){drawCard()};foes[2]=makeCard();foes[0]=makeCard();message="Escolha uma carta e depois um espaço vazio";invalidate()}
  private fun makeCard():UnitCard=UnitCard(names.random(),Random.nextInt(70,121),Random.nextInt(20,46))
  private fun drawCard(){if(reserve.isNotEmpty()&&hand.size<5)hand.add(reserve.removeAt(0))}
  private fun result(win:Boolean){page=if(win)"win" else "lose";invalidate()}
  private fun endTurn(){for(i in foes.indices){val enemy=foes[i]?:continue;val targets=allies.indices.filter{allies[it]!=null};if(targets.isNotEmpty()){val j=targets.random();val target=allies[j]!!;target.hp-=enemy.atk;if(target.hp<=0){allies[j]=null;lost++}}};if(lost>=5){result(false);return};if(foes.count{it!=null}<5){val slot=foes.indexOfFirst{it==null};if(slot>=0)foes[slot]=makeCard()};allies.filterNotNull().forEach{it.used=false};drawCard();turn++;attacker=-1;chosen=-1;message="Turno $turn: sua vez";invalidate()}
  private fun attack(a:Int,b:Int){val me=allies[a]?:return;val enemy=foes[b]?:return;if(me.used){message="Essa criatura já atacou";invalidate();return};enemy.hp-=me.atk;me.used=true;message="${me.name} causou ${me.atk} de dano em ${enemy.name}!";if(enemy.hp<=0){foes[b]=null;defeated++;if(defeated>=5){result(true);return}};attacker=-1;invalidate()}
  override fun onDraw(canvas:Canvas){super.onDraw(canvas);canvasRef=canvas;val sx=width/960f;val sy=height/540f;canvas.save();canvas.scale(sx,sy);canvas.drawColor(color("#081226"));hit.clear();if(page=="menu"&&menuArt!=null){menuV02();canvas.restore();return};if(page=="creator"&&creatorArt!=null){creatorV02();canvas.restore();return};paint.color=color("#102C4C");canvas.drawCircle(480f,260f,220f,paint);txt("CHAOTIC  |  CARD RPG",28f,48f,33f,color("#FF6262"));when(page){
   "menu"->{txt("O CÓDIGO CHAOTIC",320f,145f,42f);txt("Seu mundo. Suas cartas. Seu duelo.",322f,185f,22f);button("NOVO JOGO",340f,240f,280f,55f){start()};button("COMO JOGAR",340f,308f,280f,55f){page="help";invalidate()};txt("Protótipo v0.1 — artes provisórias",330f,465f,19f)}
   "help"->{txt("COMO JOGAR",310f,120f,35f);listOf("1. Selecione uma carta da mão.","2. Toque em uma posição livre no seu campo.","3. Toque na criatura aliada e depois no inimigo.","4. Finalize o turno para o adversário jogar.").forEachIndexed{i,s->txt(s,175f,180f+i*44,23f)};button("VOLTAR",340f,420f,240f,48f){page="menu";invalidate()}}
   "creator"->{txt("CRIE SEU DUELISTA",290f,110f,37f);txt("Nome: $player",300f,175f,28f);button("EDITAR NOME",300f,195f,300f,48f){val input=EditText(this@MainActivity);input.setText(player);AlertDialog.Builder(this@MainActivity).setTitle("Nome do duelista").setView(input).setPositiveButton("Salvar"){_,_->player=input.text.toString().take(18).ifBlank{"Duelista"};invalidate()}.setNegativeButton("Cancelar",null).show()};txt("Estilo: ${listOf("Vermelho","Azul","Verde")[style]}",300f,290f,26f);button("MUDAR VISUAL",300f,310f,300f,48f){style=(style+1)%3;invalidate()};button("CONFIRMAR",300f,390f,300f,55f){scene=0;page="story";invalidate()}}
   "story"->{txt("CAPÍTULO 1 — BEM-VINDO AO CHAOTIC",155f,112f,32f);drawBox(115f,180f,730f,165f,color("#13253E"));txt(dialogues[scene],140f,262f,25f);button(if(scene==dialogues.lastIndex)"INICIAR DUELO" else "CONTINUAR",320f,380f,310f,60f){if(scene==dialogues.lastIndex)duel() else {scene++;invalidate()}}}
   "duel"->{txt("TURNO $turn",795f,45f,24f);txt("KAZ — CAMPO ADVERSÁRIO",315f,79f,20f);for(i in 0..4)drawCardSlot(i,true);txt("$player — SEU CAMPO",340f,290f,20f);for(i in 0..4)drawCardSlot(i,false);txt("MÃO DE CARTAS",25f,445f,20f);for(i in hand.indices){val x=200f+i*118;val c=hand[i];drawBox(x,420f,110f,70f,if(chosen==i)color("#75431E") else color("#173654"));txt(c.name.take(10),x+8,447f,18f);txt("${c.atk} ATK",x+8,471f,16f);hit.add(RectF(x,420f,x+110f,490f) to {chosen=i;attacker=-1;message="${c.name}: escolha espaço no seu campo";invalidate()})};button("FIM TURNO",785f,445f,160f,48f){endTurn()};txt(message.take(78),30f,520f,19f,color("#FFDE9B"))}
   "win","lose"->{txt(if(page=="win")"VITÓRIA!" else "DERROTA!",360f,175f,50f);txt(if(page=="win")"Você venceu Kaz!" else "Kaz venceu este duelo.",330f,245f,27f);button("JOGAR NOVAMENTE",320f,315f,330f,58f){duel()};button("MENU",370f,395f,220f,50f){page="menu";invalidate()}}
  };canvas.restore()}
  private fun drawCardSlot(i:Int,enemy:Boolean){val x=185f+i*125f;val y=if(enemy)100f else 310f;val c=if(enemy)foes[i] else allies[i];drawBox(x,y,113f,100f,if(enemy)color("#4A1D2A") else color("#163A4E"),if((!enemy&&attacker==i)||(!enemy&&chosen>=0&&c==null))color("#FFCF56") else color("#66BDE7"));if(c==null){txt("+",x+46,y+59,32f)}else{txt(c.name.take(10),x+8,y+31,20f);txt("HP ${c.hp}",x+8,y+57,18f);txt("ATK ${c.atk}",x+8,y+81,18f)};hit.add(RectF(x,y,x+113f,y+100f) to {if(enemy){if(attacker>=0)attack(attacker,i)}else if(c==null&&chosen>=0){allies[i]=hand.removeAt(chosen);chosen=-1;message="Criatura invocada. Toque nela para atacar.";invalidate()}else if(c!=null){attacker=i;chosen=-1;message="${c.name}: toque em uma carta inimiga";invalidate()}})}
  override fun onTouchEvent(event:android.view.MotionEvent):Boolean{if(event.action!=MotionEvent.ACTION_UP)return true;val x=event.x*960f/width;val y=event.y*540f/height;hit.asReversed().firstOrNull{it.first.contains(x,y)}?.second?.invoke();return true}
 }
}
