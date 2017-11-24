# EmotionKeyboard

   类似微信聊天界面的表情窗口控制，窗口跟软键盘同高，无缝切换效果。
   核心代码参考的是dss886的开源项目（ https://github.com/dss886/Android-EmotionInputDetector ）。不过增强了扩展性：
   <ul>
       <li>新增 KeyboardInfo 类，封装并能很方便获取软键盘相关信息和状态。</li>
       <li>可以添加多个表情布局并进行切换。</li>
       <li>默认实现了聊天内容布局在LinearLayout中锁定的处理，如果你的聊天内容布局不在
       LinearLayout中，则可以自己实现 ContentViewLocker 来处理。</li>
       <li>提供 touchContentViewHideAllEnabled(View.OnTouchListener) 方法，调用后
       触摸聊天内容区域可获取焦点，收起键盘。</li>
       <li>...</li>
   </ul>
   也修复了几个bug：
   <ul>
       <li>第一次进入app，点击表情按钮，再点击输入框弹出键盘，此时表情工具条没有顶在键盘
       上（没有requestLayout导致）</li>
       <li>第一次进入app，点击表情按钮，点击输入框弹出键盘，收起键盘，再点击表情按钮，表情布局高
       度还是不正确（因为源码是只有在点击表情按钮收键盘才会记录键盘高度）</li>
       <li>...</li>
   </ul>
   做了一些优化：
   <ul>
	  <li>开放了一些常用api</li>
	  <li>使用Build模式来配置</li>
	  <li>...</li>
  </ul>
  
  
---
  GIF和图片预览：

  ![gif](https://raw.githubusercontent.com/missmess/EmotionKeyboard/master/raw/sample.gif)
  
  ![gif](https://raw.githubusercontent.com/missmess/EmotionKeyboard/master/raw/sample2.gif)

---

### 如何添加到项目中

在项目的build.gradle中添加该dependencies：

  `
    compile 'com.missmess.emotionkeyboard:emotionkeyboard:1.0.0'
  `

---

### 功能介绍

#### KeyboardInfo
  
  KeyboardInfo可以用来监听键盘状态变化和获取键盘高度信息，并缓存。使用步骤：
  
  1. 在onCreate中获取：
  ```java
  KeyboardInfo keyboardInfo = KeyboardInfo.from(this);
  ```
  
  2. 设置监听listener:
  ```java
    keyboardInfo.setOnKeyboardChangeListener(new KeyboardInfo.OnSoftKeyboardChangeListener() {
		@Override
		public void onSoftKeyboardStateChanged(boolean shown, int height) {
			if (shown) {
				tv_status.setText(String.format("键盘弹出了，高度为 %d", height));
			} else {
				tv_status.setText("键盘收起了");
			}
		}
	});
  ```
  
  3. 在onCreate或者onResume中开始监听键盘状态变化：
  ```java
    keyboardInfo.startListening();
  ```
  
  4. 在onDestory或者onPause中结束监听：
  ```java
    keyboardInfo.stopListening();
  ```
  
  也可以通过调用getSoftKeyboardHeight()方法，只要打开过一次键盘之后这个方法就总能取到正确的键盘高度值。
  
#### EmotionKeyboard

  帮助协调聊天内容布局，聊天输入框，表情按钮和表情布局的控制类。这个类的作用有：
  
  1. 实现弹出键盘和表情布局；
  2. 实现表情布局和软键盘同高，无缝切换；
  3. 实现输入框和内容布局的焦点控制。
  
  实现效果类似微信的聊天输入框交互，并能实现很多的自定义扩展功能。
  
  使用步骤：
  1. 使用Builder创建和定义你需要的功能：
  ```java
  			  emotionKeyboard = new EmotionKeyboard.Builder(this)
                  .contentLayout(contentView)//绑定内容view
                  .editText(editText)//绑定EditView
                  .addEmotionBtnAndLayout(emoji_button1, layout_button1)//添加第一个表情按钮布局
                  .addEmotionBtnAndLayout(emoji_button2, layout_button2)//第二个
                  .touchContentViewHideAllEnabled(null)//是否在触摸内容view时获取焦点隐藏键盘
                  .keyboardStateCallback(callback)//键盘状态监听
                  .emotionPanelStateCallback(callback)//表情布局状态监听
                  .build();//创建
  ```
  
  2. 注意在按下返回键时判断是否要隐藏表情布局：
  ```java
      @Override
      public void onBackPressed() {
          if (!emotionKeyboard.interceptBackPress()) {
              super.onBackPressed();
          }
      }
  ```
  
  <b>参考DEMO中的WechatActivity，可以完全的实现类似微信聊天界面的交互。</b>
  
### 关于作者
在使用中有任何问题，欢迎反馈给我，可以用以下联系方式跟我交流：

* 邮箱：<tarcy3620@126.com>
* GitHub: [@missmess](https://github.com/missmess)

---

###### CopyRight：`missmess`