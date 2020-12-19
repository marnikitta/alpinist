function autoGrowTextField(element) {
  element.style.height = "5px"
  element.style.height = (element.scrollHeight + 27 * 2)+"px";
}