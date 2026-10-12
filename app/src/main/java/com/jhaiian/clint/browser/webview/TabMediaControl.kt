package com.jhaiian.clint.browser.webview

object TabMediaControl {
    const val PAUSE_SCRIPT = "(function(){function p(d){try{d.querySelectorAll('video,audio').forEach(function(m){try{m.pause()}catch(e){}});d.querySelectorAll('iframe').forEach(function(f){try{if(f.contentDocument)p(f.contentDocument)}catch(e){}})}catch(e){}}p(document)})();"

    private const val MUTE_TEMPLATE = "(function(m){var w=window;var s=w.__clintTabMute;if(!s){s={m:false};var ap=function(e){try{if(s.m){if(!e.__cmx){e.__cmx=1;e.__cmo=e.muted}if(!e.muted)e.muted=true}else if(e.__cmx){e.__cmx=0;e.muted=e.__cmo}}catch(x){}};s.all=function(){try{document.querySelectorAll('video,audio').forEach(ap)}catch(x){}};try{var op=HTMLMediaElement.prototype.play;HTMLMediaElement.prototype.play=function(){ap(this);return op.apply(this,arguments)}}catch(x){}['play','playing','volumechange','loadedmetadata'].forEach(function(t){document.addEventListener(t,function(ev){var e=ev.target;if(s.m&&e&&(e.tagName==='VIDEO'||e.tagName==='AUDIO')&&!e.muted)ap(e)},true)});try{new MutationObserver(function(){if(s.m)s.all()}).observe(document,{childList:true,subtree:true})}catch(x){}try{Object.defineProperty(w,'__clintTabMute',{value:s,configurable:true})}catch(x){w.__clintTabMute=s}}s.m=m;s.all()})(__MUTED__);"

    fun muteScript(muted: Boolean): String = MUTE_TEMPLATE.replace("__MUTED__", muted.toString())
}
