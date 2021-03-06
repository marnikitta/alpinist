window.onload = function() {
    const discussionEdit = document.getElementById('discussion-edit');
    if (discussionEdit) {
        autoGrowTextField(discussionEdit);
    }

    changeTitleFontSize();

    return false;
}

window.onresize = function(event) {
    changeTitleFontSize();
}

function changeTitleFontSize() {
    const spaceTitle = document.getElementById('space-title')
    if (!spaceTitle) {
        return false;
    }

    spaceTitle.style.fontSize = titleFontSize(spaceTitle.innerText);
    return false;
}

function titleFontSize(text) {
    const ruler = document.getElementById("ruler");
    ruler.innerText = text;

    var i = 20;
    for (; i < 100; i += 2) {
        ruler.style.fontSize = i + 'px';
        if (ruler.offsetHeight > 160) {
            break
        }
    }
    return i + 'px';
}
function autoGrowTextField(element) {
    element.style.height = '5px';
    element.style.height = (element.scrollHeight + 27 * 2) + "px";
    return false;
}

//function highlightMatchedTags(elementsParent, text) {
//    let allTags = elementsParent.querySelectorAll('.suggested-tag');
//    for (let tag of allTags) {
//        if (tag.innerText.includes(text)) {
//            tag.hidden = false;
//        } else {
//            tag.hidden = true;
//        }
//    }
//}

function addOutlink(outlinkName) {
    let discussionEdit = document.getElementById('discussion-edit');
    let discussionValue = discussionEdit.value

    if (discussionValue.length != 0 && discussionValue[discussionValue.length - 1] != '\n') {
        discussionValue += ', '
    }
    discussionValue += '[[' + outlinkName + ']]'

    discussionEdit.value = discussionValue

    autoGrowTextField(discussionEdit)
//    discussionEdit.setSelectionRange(discussionEdit.value.length, discussionEdit.value.length);
//    discussionEdit.focus()
    return false;
}

function clearEdit() {
    document.getElementById('space-title-url').value = '';
    document.getElementById('discussion-edit').value = '';
    autoGrowTextField(document.getElementById('discussion-edit'));
}

function addReferrer(formElement) {
    let input = document.createElement("input");
    input.setAttribute("type", "hidden");
    input.setAttribute("name", "referrer");
    input.setAttribute("value", document.referrer);
    formElement.appendChild(input);
    return true;
}

function toggleGroupBody(id) {
    let groupBody = document.getElementById(id).querySelector('.link-group-body');
    groupBody.hidden = !groupBody.hidden;
}