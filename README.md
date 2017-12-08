# EmotionKeyboard

   类似微信聊天界面的表情窗口控制，窗口跟软键盘同高，无缝切换效果。
   有两个类，原先的EmotionKeyboard使用的核心代码参考的是dss886的开源项目（ https://github.com/dss886/Android-EmotionInputDetector ）。
   但是经测试发现，这个类有一些无法解决的问题（源于它的方法实现原理），如
   <ol>
   <li>表情键盘显示时，长按输入框文字会导致表情键盘，软键盘切换出错。</li>
   <li>表情键盘和软键盘无法平滑的过渡。</li>
   <li>使用不太方便，默认只支持LinearLayout，要自己实现locker</li>
   </ol>
   所以这个类已经不提供更新了，仅仅提供大家作为参考。
   现在用了一个新的思路去实现同样的功能。使用方法相同，但是却解决了以上无法解决的所有问题。详情请查看EmojiconKeyBoard源码。
  
  
---
  GIF和图片预览：

  ![gif](https://raw.githubusercontent.com/missmess/EmotionKeyboard/master/raw/sample.gif)
  
  ![gif](https://raw.githubusercontent.com/missmess/EmotionKeyboard/master/raw/sample2.gif)

---

### 如何添加到项目中

在项目的build.gradle中添加该dependencies：

  `
    compile 'com.missmess.emotionkeyboard:emotionkeyboard:1.1.0'
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
  
#### EmojiconKeyBoard

  帮助协调聊天内容布局，聊天输入框，表情按钮和表情布局的控制类。这个类的作用有：
  
  1. 实现弹出键盘和表情布局；
  2. 实现表情布局和软键盘同高，无缝切换；
  3. 实现输入框和内容布局的焦点控制。
  
  实现效果类似微信的聊天输入框交互，并能实现很多的自定义扩展功能。
  
  使用步骤：
  1. 使用Builder创建和定义你需要的功能：
  ```java
  			  emotionKeyboard = new EmojiconKeyBoard.Builder(this)
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
  
#### EmojiconKeyBoard

  已经废弃，仅供参考，不要使用。已经使用的请使用新的类EmojiconKeyBoard
  
### 关于作者
在使用中有任何问题，欢迎反馈给我，可以用以下联系方式跟我交流：

* 邮箱：<tarcy3620@126.com>
* GitHub: [@missmess](https://github.com/missmess)

---

###### CopyRight：`missmess`