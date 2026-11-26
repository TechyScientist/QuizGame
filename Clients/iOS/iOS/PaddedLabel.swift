import UIKit

@IBDesignable
class PaddedLabel: UILabel {
    @IBInspectable var topInset: CGFloat = 12.0
    @IBInspectable var bottomInset: CGFloat = 12.0
    @IBInspectable var leftInset: CGFloat = 14.0
    @IBInspectable var rightInset: CGFloat = 14.0
    @IBInspectable var cornerRadius: CGFloat = 10.0

    override func layoutSubviews() {
        super.layoutSubviews()
        layer.cornerRadius = cornerRadius
        layer.masksToBounds = true
    }

    override func drawText(in rect: CGRect) {
        let insets = UIEdgeInsets(top: topInset, left: leftInset, bottom: bottomInset, right: rightInset)
        super.drawText(in: rect.inset(by: insets))
    }

    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(
            width: size.width + leftInset + rightInset,
            height: size.height + topInset + bottomInset
        )
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let adjusted = CGSize(width: size.width - leftInset - rightInset, height: size.height - topInset - bottomInset)
        let fitted = super.sizeThatFits(adjusted)
        return CGSize(
            width: fitted.width + leftInset + rightInset,
            height: fitted.height + topInset + bottomInset
        )
    }
}
