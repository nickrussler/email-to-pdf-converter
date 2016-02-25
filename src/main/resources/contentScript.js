/*
 * Copyright 2016 Nick Russler
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	f = document.createElement('iframe');
	f.src = document.getElementById('header-v6a8oxpf48xfzy0rhjra').getAttribute('data-file');
	f.border = 0;
	f.id = 'header-frame-6jk3h6a234g2a3';
	var fs = f.style;
	fs.position = 'absolute';
	fs.overflow = 'hidden';
	fs.width = '1000px';
	fs.top = 0;
	fs.left = 0;
	fs.zIndex = 100003;
	fs.border = 'none';
	fs.padding = 0;
	fs.margin = 0;
	
	document.body.insertBefore(f, document.body.firstChild);

	var oldVal = parseInt(document.getElementsByTagName('html')[0].style.paddingTop) || 0;
	
	window.addEventListener('message', function(event) {
		document.getElementById('header-frame-6jk3h6a234g2a3').height = event.data;
		document.getElementsByTagName('html')[0].style.paddingTop = (oldVal + event.data) + 'px';
		
		// TODO push down all position absolute elements and remeber their old positions
//		var all = document.querySelector('*');
//		for(var i = 0; i < all.length; i++) {
//		  if (all[i].style.position == 'absolute' && all[i] !== f) {
//		    all[i].style.top = ((parseInt(all[i].style.top) || 0) + event.data) + 'px';
//		  }
//		}
	});
})();