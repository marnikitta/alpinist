window.onload = function() {
    let discussionEdit = document.getElementById('discussion-edit');
    if (discussionEdit) {
        autoGrowTextField(discussionEdit);
    }
    return false;
}

function autoGrowTextField(element) {
    element.style.height = '5px';
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
    discussionEdit.setSelectionRange(discussionEdit.value.length, discussionEdit.value.length);
    discussionEdit.focus()
    return false;
}

function clearEdit() {
    document.getElementById('space-title-url').value = '';
    document.getElementById('discussion-edit').value = '';
}
