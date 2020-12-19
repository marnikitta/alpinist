function autoGrowTextField(element) {
    element.style.height = "5px"
    element.style.height = (element.scrollHeight + 27 * 2) + "px";
    return false;
}

function addOutlink(outlinkName) {
    let discussionEdit = document.getElementById('discussion-edit');
    let discussionValue = discussionEdit.value

    if (discussionValue.length != 0 && discussionValue[discussionValue.length - 1] != '\n') {
        discussionValue += ', '
    }
    discussionValue += '[[' + outlinkName + ']]'

    discussionEdit.value = discussionValue

    autoGrowTextField(discussionEdit)
    return false;
}
